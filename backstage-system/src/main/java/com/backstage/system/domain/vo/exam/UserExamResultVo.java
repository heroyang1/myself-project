package com.backstage.system.domain.vo.exam;

import java.io.Serializable;
import java.util.List;

/**
 * Exam result detail for the post-submit analysis page.
 */
public class UserExamResultVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer answer_status;
    private Integer read_status;
    private Integer score;
    private Integer correct_count;
    private Integer wrong_count;
    private Boolean passed;
    private ExampaperInfoVo testpaper;
    private List<QuestionVo> questions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getAnswer_status() { return answer_status; }
    public void setAnswer_status(Integer answer_status) { this.answer_status = answer_status; }
    public Integer getRead_status() { return read_status; }
    public void setRead_status(Integer read_status) { this.read_status = read_status; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getCorrect_count() { return correct_count; }
    public void setCorrect_count(Integer correct_count) { this.correct_count = correct_count; }
    public Integer getWrong_count() { return wrong_count; }
    public void setWrong_count(Integer wrong_count) { this.wrong_count = wrong_count; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public ExampaperInfoVo getTestpaper() { return testpaper; }
    public void setTestpaper(ExampaperInfoVo testpaper) { this.testpaper = testpaper; }
    public List<QuestionVo> getQuestions() { return questions; }
    public void setQuestions(List<QuestionVo> questions) { this.questions = questions; }
}
