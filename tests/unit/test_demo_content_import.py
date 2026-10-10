import copy
import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('content_import', ROOT / 'scripts/update-demo-content.py')
importer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(importer)


class FakeApi:
    def __init__(self, catalog, baseline):
        self.catalog = catalog
        self.old = baseline['courses'][0]
        self.teacher = dict(username=catalog['accounts'][1]['username'], userId=42, isActive=True)
        self.course = dict(courseId=91, teacherId=42, title=self.old['courseTitle'],
                           description=self.old['baseline']['description'],
                           learningOutcomes=self.old['baseline']['learningOutcomes'])
        self.lessons = [dict(row, lessonId=101 + index, contentRevision=0, contentUrl=None)
                        for index, row in enumerate(self.old['baseline']['lessons'])]
        self.quiz = None

    def request(self, path):
        if path == '/users?role=TEACHER':
            return [self.teacher]
        if path == '/courses':
            return [self.course]
        if path == '/courses/91':
            return self.course
        if path == '/courses/91/lessons':
            return self.lessons
        if path.endswith('/quiz'):
            return self.quiz
        raise AssertionError(path)


class ContentImportTests(unittest.TestCase):
    def setUp(self):
        self.catalog = importer.load(ROOT / 'src/main/resources/demo/catalog.json')
        self.baseline = importer.load(ROOT / 'src/main/resources/demo/content-update-baseline.json')
        self.baseline['courses'] = self.baseline['courses'][:1]
        self.api = FakeApi(self.catalog, self.baseline)

    def test_preserves_instructor_fields_and_existing_quiz(self):
        self.api.course['description'] = 'Instructor description'
        self.api.lessons[1]['textContent'] = 'Instructor content'
        self.api.lessons[2]['contentRevision'] = 3  # Even restored baseline is an intentional edit.
        self.api.quiz = dict(revision=2, title='Instructor quiz')
        actions, skips = importer.plan(self.api, self.catalog, self.baseline)
        course = next(row for row in actions if row['kind'] == 'course')
        self.assertEqual(course['body']['description'], 'Instructor description')
        self.assertEqual(course['changedFields'], ['learningOutcomes'])
        self.assertEqual([row['before']['lessonId'] for row in actions if row['kind'] == 'lesson'], [101, 104])
        self.assertFalse(any(row['kind'] == 'quiz' for row in actions))
        self.assertEqual(len(skips), 3)

    def test_keeps_added_links_and_passes_revision_to_lesson_api(self):
        self.api.lessons[0]['contentUrl'] = 'https://example.invalid/teacher-document'
        actions, _ = importer.plan(self.api, self.catalog, self.baseline)
        body = next(row['body'] for row in actions if row['kind'] == 'lesson')
        self.assertEqual(body['contentUrl'], self.api.lessons[0]['contentUrl'])
        self.assertEqual(body['expectedRevision'], 0)
        self.assertNotIn('isPublished', body)
        self.assertNotIn('draftRevision', body)

    def test_second_import_has_no_actions_and_keeps_history(self):
        sample = self.catalog['baseCourses'][0]
        self.api.course.update({key: sample[key] for key in ('description', 'learningOutcomes')})
        for lesson, replacement in zip(self.api.lessons, sample['lessons'], strict=True):
            lesson.update(replacement)
            lesson['contentRevision'] = 1
        self.api.quiz = dict(sample['quiz'], revision=1)
        actions, _ = importer.plan(self.api, self.catalog, self.baseline)
        self.assertEqual(actions, [])

    def test_deleted_or_ambiguous_lessons_are_not_recreated(self):
        self.api.lessons.pop()
        self.api.lessons.append(copy.deepcopy(self.api.lessons[0]))
        actions, _ = importer.plan(self.api, self.catalog, self.baseline)
        self.assertEqual(sum(row['kind'] == 'lesson' for row in actions), 2)
        self.assertFalse(any(row['kind'] == 'quiz' for row in actions))

    def test_other_teacher_with_same_title_is_not_updated(self):
        self.api.course['teacherId'] = 99
        actions, _ = importer.plan(self.api, self.catalog, self.baseline)
        self.assertEqual(actions, [])

    def test_expected_revision_comes_from_update_data(self):
        old = self.baseline['courses'][0]['baseline']['lessons'][0]
        old['contentRevision'] = 4
        self.api.lessons[0]['contentRevision'] = 4
        actions, _ = importer.plan(self.api, self.catalog, self.baseline)
        action = next(row for row in actions if row['kind'] == 'lesson' and row['before']['lessonId'] == 101)
        self.assertEqual(action['body']['expectedRevision'], 4)


if __name__ == '__main__':
    unittest.main()
