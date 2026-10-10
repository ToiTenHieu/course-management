"""Import JSON demo content through existing APIs; dry-run unless --apply is given.

No course IDs, names, lesson bodies or quiz answers belong in this importer.
The catalog provides new content; the baseline file identifies untouched old data.
"""
import argparse
import http.cookiejar
import json
import os
from pathlib import Path
import sys
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
COURSE_FIELDS = ('title', 'teacherId', 'description', 'durationHours', 'category',
                 'level', 'learningOutcomes', 'prerequisites', 'targetAudience')
LESSON_FIELDS = ('title', 'textContent', 'contentFormat', 'orderIndex', 'contentUrl', 'videoUrl')


def load(path):
    return json.loads(Path(path).read_text(encoding='utf-8'))


class Api:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip('/') + '/api'
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.csrf = None

    def request(self, path, method='GET', body=None):
        headers = {'Accept': 'application/json'}
        if body is not None:
            headers['Content-Type'] = 'application/json; charset=utf-8'
        if method != 'GET':
            if self.csrf is None:
                self.csrf = self.request('/auth/csrf')
            headers[self.csrf['headerName']] = self.csrf['token']
        request = urllib.request.Request(
            self.base_url + path, method=method, headers=headers,
            data=None if body is None else json.dumps(body, ensure_ascii=False).encode('utf-8'))
        try:
            with self.opener.open(request, timeout=30) as response:
                result = json.load(response)
        except urllib.error.HTTPError as error:
            raise RuntimeError(f'{method} {path}: HTTP {error.code}') from error
        if not result.get('success'):
            raise RuntimeError(f'{method} {path}: {result.get("message")}')
        return result.get('data')

    def login(self, username, password):
        if self.request('/auth/config').get('demo') is not True:
            raise RuntimeError('This dataset is only intended for a demo server.')
        profile = self.request('/auth/login', 'POST', dict(username=username, password=password))
        self.csrf = None  # Login rotates the session and CSRF token.
        if profile['role'] != 'ADMIN':
            raise RuntimeError('An admin session is required to resolve demo teachers and courses.')


def course_changes(current, baseline, replacement):
    """Replace individual fields only when they still equal the original sample."""
    return {key: replacement[key] for key, old in baseline.items()
            if key in COURSE_FIELDS and key not in ('title', 'teacherId')
            and key in replacement and current.get(key) == old
            and current.get(key) != replacement[key]}


def lesson_body(current, baseline, replacement):
    # Preserve the entire lesson when any editable content differs from the old sample.
    # The expected revision belongs to the dataset, so later update packs can use
    # another baseline without changing this importer.
    if current.get('contentRevision') != baseline.get('contentRevision'):
        return None
    guard = ('title', 'textContent', 'contentFormat', 'orderIndex', 'videoUrl')
    if any(current.get(key) != baseline.get(key) for key in guard):
        return None
    body = {key: current.get(key) for key in LESSON_FIELDS}
    for key in ('title', 'textContent', 'contentFormat'):
        body[key] = replacement[key]
    if all(body.get(key) == current.get(key) for key in LESSON_FIELDS):
        return None
    body['expectedRevision'] = current['contentRevision']
    return body


def plan(api, catalog, updates):
    teachers = api.request('/users?role=TEACHER')
    courses = api.request('/courses')
    actions, skips = [], []
    account_specs = {row['key']: row for row in catalog['accounts']}
    for entry in updates['courses']:
        spec = account_specs[entry['teacher']]
        matches = [row for row in teachers if row['username'] == spec['username'] and row['isActive']]
        if len(matches) != 1:
            skips.append(dict(course=entry['courseTitle'], reason='missing or ambiguous active teacher'))
            continue
        teacher_id = matches[0]['userId']
        matches = [row for row in courses if row['teacherId'] == teacher_id
                   and row['title'] == entry['courseTitle']]
        if len(matches) != 1:
            skips.append(dict(course=entry['courseTitle'], reason='missing or ambiguous course'))
            continue
        current = api.request(f'/courses/{matches[0]["courseId"]}')
        if 'catalogGroup' in entry:
            samples = [row for row in catalog[entry['catalogGroup']]
                       if row['title'] == entry['courseTitle'] and row['teacher'] == entry['teacher']]
            if len(samples) != 1:
                raise ValueError('Missing or ambiguous replacement in catalog: ' + entry['courseTitle'])
            replacement = samples[0]
        else:
            replacement = entry['replacement']
        changes = course_changes(current, entry['baseline'], replacement)
        if changes:
            body = {key: current.get(key) for key in COURSE_FIELDS}
            body.update(changes)
            actions.append(dict(kind='course', path=f'/courses/{current["courseId"]}',
                                before=current, body=body, changedFields=list(changes)))
        course_lessons = api.request(f'/courses/{current["courseId"]}/lessons')
        updated_orders = set()
        for old in entry['baseline'].get('lessons', []):
            matches = [row for row in course_lessons if row['orderIndex'] == old['orderIndex']]
            new = [row for row in replacement.get('lessons', []) if row['orderIndex'] == old['orderIndex']]
            if len(matches) != 1 or len(new) != 1:
                skips.append(dict(course=current['title'], order=old['orderIndex'], reason='missing or ambiguous lesson'))
                continue
            actual = matches[0]
            body = lesson_body(actual, old, new[0])
            # Recognize an already imported lesson without writing another revision.
            same = all(actual.get(key) == new[0].get(key) for key in ('title', 'textContent', 'contentFormat'))
            if body:
                actions.append(dict(kind='lesson', path=f'/lessons/{actual["lessonId"]}',
                                    before=actual, body=body))
            if body or same:
                updated_orders.add(old['orderIndex'])
            else:
                skips.append(dict(course=current['title'], order=old['orderIndex'], reason='instructor edits preserved'))
        # CourseSample quizzes target the last sample lesson, exactly as bootstrap does.
        quiz = replacement.get('quiz')
        if quiz and replacement.get('lessons'):
            last_order = replacement['lessons'][-1]['orderIndex']
            matches = [row for row in course_lessons if row['orderIndex'] == last_order]
            if len(matches) == 1 and last_order in updated_orders:
                path = f'/lessons/{matches[0]["lessonId"]}/quiz'
                existing = api.request(path)
                if existing is None:
                    body = dict(quiz, expectedRevision=0, published=True)
                    actions.append(dict(kind='quiz', path=path, before=None, body=body))
                else:
                    skips.append(dict(course=current['title'], reason='existing quiz preserved'))
    return actions, skips


def run(args):
    catalog, updates = load(args.catalog), load(args.baseline)
    password = os.environ.get(args.password_env)
    if not password:
        raise ValueError('Set ' + args.password_env + ' to the current admin password.')
    api = Api(args.base_url)
    api.login(args.username, password)
    report = dict(dataset=updates['dataset'], applied=args.apply, actions=[], skips=[])
    report_path = Path(args.report)
    report_path.parent.mkdir(parents=True, exist_ok=True)

    def save():
        report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

    try:
        actions, report['skips'] = plan(api, catalog, updates)
        report['actions'] = actions
        save()  # Store original content before the first write.
        for action in actions:
            if not args.apply:
                action['result'] = 'dry-run'
                continue
            # Re-read before each write. Lesson and quiz APIs also enforce revisions.
            latest = api.request(action['path'])
            guard = COURSE_FIELDS if action['kind'] == 'course' else LESSON_FIELDS + ('contentRevision',)
            unchanged = latest is None if action['kind'] == 'quiz' else (
                latest is not None and all(latest.get(key) == action['before'].get(key) for key in guard))
            if not unchanged:
                action['result'] = 'skipped: changed after planning'
            else:
                action['after'] = api.request(action['path'], 'PUT', action['body'])
                action['result'] = 'applied'
            save()
        save()
    except Exception as error:
        report['error'] = str(error)
        save()
        raise
    finally:
        api.request('/auth/logout', 'POST')
    counts = {kind: sum(row['kind'] == kind for row in report['actions']) for kind in ('course', 'lesson', 'quiz')}
    applied = sum(row.get('result') == 'applied' for row in report['actions'])
    print(json.dumps(dict(mode='apply' if args.apply else 'dry-run', counts=counts,
                          applied=applied, skipped=len(report['skips']),
                          report=str(report_path.resolve())), ensure_ascii=True))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', required=True)
    parser.add_argument('--username', required=True)
    parser.add_argument('--password-env', default='COURSE_IMPORT_PASSWORD')
    parser.add_argument('--catalog', type=Path, default=ROOT / 'src/main/resources/demo/catalog.json')
    parser.add_argument('--baseline', type=Path, default=ROOT / 'src/main/resources/demo/content-update-baseline.json')
    parser.add_argument('--report', type=Path, default=ROOT / 'output/data-import/report.json')
    parser.add_argument('--apply', action='store_true')
    try:
        run(parser.parse_args())
    except (RuntimeError, ValueError, OSError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
