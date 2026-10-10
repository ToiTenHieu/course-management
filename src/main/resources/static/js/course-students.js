import { dateTime } from './dates.js';
import { mountPagedList } from './lists.js';
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const labels = {ENROLLED:'Đang học',COMPLETED:'Hoàn thành',DROPPED:'Ngừng học'};
const number = value => Number(value).toLocaleString('vi-VN',{maximumFractionDigits:1});

export async function mountCourseStudents(courseId) {
  const section = document.createElement('section');
  section.className = 'panel'; section.id = 'students';
  section.innerHTML = `<div class="section-heading"><div><h2>Học viên trong khóa</h2><p>Theo dõi tiến độ và những câu hỏi cần phản hồi.</p></div></div><div id="courseStudentSummary" class="student-summary"></div><div class="catalog-toolbar"><label class="search-box"><input type="search" id="courseStudentSearch" aria-label="Tìm học viên trong khóa" placeholder="Tên hoặc tài khoản học viên"></label><select id="courseStudentStatus" aria-label="Trạng thái học viên"><option value="">Tất cả trạng thái</option><option value="ENROLLED">Đang học</option><option value="COMPLETED">Hoàn thành</option><option value="DROPPED">Ngừng học</option></select><select id="courseStudentSort" aria-label="Sắp xếp học viên"><option value="name">Tên A–Z</option><option value="progress">Tiến độ giảm dần</option></select></div><p class="hint">Điểm quiz tính trên mọi lượt nộp trong khóa, kể cả phiên bản cũ. Hoạt động gần nhất gồm mở bài, làm quiz và gửi câu hỏi/phản hồi.</p><p id="courseStudentCount" class="result-count" role="status" aria-live="polite"></p><div id="courseStudentRows" class="student-report-list"></div><nav id="courseStudentPager" class="pagination" aria-label="Phân trang học viên trong khóa"></nav>`;
  document.querySelector('#reviews').before(section);
  const tab = document.createElement('a'); tab.href = '#students'; tab.textContent = 'Học viên';
  document.querySelector('.detail-tabs').append(tab);
  let publishedLessons = 0;
  await mountPagedList({
    endpoint: `/courses/${courseId}/students`, container:'#courseStudentRows', size:10,
    label:'học viên', countSelector:'#courseStudentCount', pagerSelector:'#courseStudentPager', syncUrl:false,
    controls:[
      {id:'#courseStudentSearch',param:'search'},
      {id:'#courseStudentStatus',param:'status',allowed:['','ENROLLED','COMPLETED','DROPPED']},
      {id:'#courseStudentSort',param:'sort',allowed:['name','progress'],default:'name'}
    ],
    unwrap: report => {
      const s = report.summary; publishedLessons = s.publishedLessons;
      document.querySelector('#courseStudentSummary').innerHTML = [['Học viên',s.students],['Đang học',s.enrolled],['Hoàn thành',s.completed],['Ngừng học',s.dropped],['Câu hỏi chờ phản hồi',s.pendingQuestions]].map(([label,value])=>`<div><strong>${number(value)}</strong><span>${label}</span></div>`).join('');
      return report.students;
    },
    render: students => {
      document.querySelector('#courseStudentRows').innerHTML = students.length ? students.map(s=>{
        const progress = Math.min(100,Math.max(0,Number(s.progressPercentage)));
        const question = s.pendingQuestionId ? `/course-detail.html?id=${courseId}&lessonId=${s.pendingLessonId}&questionId=${s.pendingQuestionId}#questions` : '';
        return `<article class="student-report-row"><div class="student-report-heading"><div><h3>${esc(s.name)}</h3><small>@${esc(s.username)}</small></div><span class="badge ${s.status==='COMPLETED'?'good':'muted'}">${labels[s.status]}</span></div><div class="student-report-progress"><span>${number(progress)}% · ${s.completedLessons}/${publishedLessons} bài đã hoàn thành</span><div class="progress-track" role="progressbar" aria-label="Tiến độ của ${esc(s.name)}" aria-valuenow="${progress}" aria-valuemin="0" aria-valuemax="100"><i style="width:${progress}%"></i></div></div><dl class="student-report-metrics"><div><dt>Quiz</dt><dd>${s.quizAttempts ? `${s.quizAttempts} lượt nộp<br><small>TB ${number(s.averageScore)}% · Cao nhất ${number(s.bestScore)}%</small>`:'Chưa làm quiz'}</dd></div><div><dt>Hoạt động gần nhất</dt><dd>${s.lastActivity ? esc(dateTime(s.lastActivity)):'Chưa có hoạt động'}</dd></div><div><dt>Câu hỏi chờ phản hồi</dt><dd>${question ? `<a class="text-link" href="${question}">${s.pendingQuestions} câu · Xem câu đầu tiên →</a>`:'Không có'}</dd></div></dl></article>`;
      }).join('') : '<div class="empty"><h3>Chưa có học viên phù hợp</h3><p>Thử đổi tên tìm kiếm hoặc trạng thái.</p></div>';
    }
  });
}
