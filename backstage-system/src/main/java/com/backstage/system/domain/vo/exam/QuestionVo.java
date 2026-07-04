package com.backstage.system.domain.vo.exam;

import java.io.Serializable;
import java.util.List;

public class QuestionVo implements Serializable {
    private Long id;
    private Integer score;
    private Long question_id;
    private String title;
    private String remark;
    private String type; // answer, completion, trueOrfalse, checkbox, radio
    private Object options; // 选项，可以是数组
    private Object user_value; // 用户填写的答案
    private Object correct_answer;
    private Boolean is_correct;
    private Integer gained_score;

    // Getters and Setters (此处简写，实际请生成完整版)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Long getQuestion_id() { return question_id; }
    public void setQuestion_id(Long question_id) { this.question_id = question_id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Object getOptions() { return options; }
    public void setOptions(Object options) { this.options = options; }
    public Object getUser_value() { return user_value; }
    public void setUser_value(Object user_value) { this.user_value = user_value; }
    public Object getCorrect_answer() { return correct_answer; }
    public void setCorrect_answer(Object correct_answer) { this.correct_answer = correct_answer; }
    public Boolean getIs_correct() { return is_correct; }
    public void setIs_correct(Boolean is_correct) { this.is_correct = is_correct; }
    public Integer getGained_score() { return gained_score; }
    public void setGained_score(Integer gained_score) { this.gained_score = gained_score; }
}