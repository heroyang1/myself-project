package com.backstage.system.service.impl.exam;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.aijudge.AiUtil;
import com.backstage.system.domain.dto.exam.UserExamSaveDto;
import com.backstage.system.domain.exam.OshUserExamRecord;
import com.backstage.system.domain.vo.exam.QuestionVo;
import com.backstage.system.domain.vo.exam.UserExamRecordVo;
import com.backstage.system.domain.vo.exam.UserExamResultVo;
import com.backstage.system.mapper.exam.OshUserExamRecordMapper;
import com.backstage.system.service.exam.IOshUserExamRecordService;
import com.backstage.system.utils.UserContextUtil;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class OshUserExamRecordServiceImpl implements IOshUserExamRecordService {
    @Autowired
    private OshUserExamRecordMapper userTestMapper;

    @Override
    public List<UserExamRecordVo> selectUserTestList(Integer page) {
        Long userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            return new ArrayList<>();
        }
        PageHelper.startPage(page, 10);
        return userTestMapper.selectUserTestList(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveUserExam(UserExamSaveDto saveDto) {
        Long examId = saveDto.getExam_id(); // 刚才我们在 DTO 里补全的字段
        Long userId = saveDto.getUser_test_id();
        OshUserExamRecord record = userTestMapper.selectByUserIdAndExamId(userId, examId);

        if (record == null) {
            // 如果不存在（即你说的 ID 为 7 但表里没数据的情况），手动创建一个存进去
            record = new OshUserExamRecord();
            record.setUser_id(saveDto.getUser_test_id());
            record.setAnswer_status(0);              // 初始为进行中
            record.setRead_status(0);                // 初始为未批阅
            record.setScore(0);
            record.setExam_id(examId);

            // 执行插入
            userTestMapper.insertOshUserExam(record);
        }
        List<QuestionVo> questions = userTestMapper.selectQuestionsByExamId(record.getExam_id());
        List<Object> userAnswers = saveDto.getValue();

        if (questions == null || userAnswers == null || questions.size() != userAnswers.size()) {
            throw new ServiceException("提交的答案数量与题目总数不匹配");
        }

        int finalScore = 0;
        // 用于存储简答题，发给 AI
        StringBuilder essayPromptBuilder = new StringBuilder();
        List<Integer> essayIndexList = new ArrayList<>(); // 记录简答题在原列表的索引，方便回填

        // 1. 自动批阅逻辑（客观题）
        for (int i = 0; i < questions.size(); i++) {
            QuestionVo q = questions.get(i);
            Object uAns = userAnswers.get(i);
            String correct = q.getCorrect_answer() != null ? String.valueOf(q.getCorrect_answer()) : "";

            // 【AI 介入点】如果是简答题 (answer)
            if ("answer".equals(q.getType())) {
                essayIndexList.add(i);
                essayPromptBuilder.append(String.format("【题目 %d】: %s\n【参考答案】: %s\n【用户回答】: %s\n【满分】: %d\n\n",
                        i + 1, q.getTitle(), correct, uAns, q.getScore()));
                continue;
            }

            if (isAnswerCorrect(q, uAns)) {
                finalScore += safeScore(q);
            }
        }

        // 2. 调用 AI 评分（处理简答题）
        if (!essayIndexList.isEmpty()) {
            String aiResponse = callAiForScoring(essayPromptBuilder.toString());
            if (aiResponse != null) {
                // 解析 AI 返回的 JSON，例如 {"scores": [15, 20], "evaluation": "..."}
                JSONObject aiResult = JSON.parseObject(aiResponse);
                JSONArray scoresArray = aiResult.getJSONArray("scores");

                if (scoresArray != null && scoresArray.size() == essayIndexList.size()) {
                    for (int j = 0; j < scoresArray.size(); j++) {
                        finalScore += scoresArray.getIntValue(j);
                    }
                }
            }
            // 注意：由于 AI 已经实时评分，read_status 可以直接设为 1（已批阅）
        }

        // 3. 构造更新对象
        record.setScore(finalScore);
        record.setAnswer_status(1);
        record.setRead_status(1); // AI 评完了，直接标记为已批阅
        record.setAnswer_json(JSON.toJSONString(userAnswers));

        return userTestMapper.updateOshUserExam(record) > 0;
    }


    @Override
    public UserExamResultVo getUserExamResult(Long recordId) {
        Long userId = UserContextUtil.getCurrentUserId();
        if (userId == null || recordId == null) {
            return null;
        }
        OshUserExamRecord record = userTestMapper.selectRecordByIdAndUserId(recordId, userId);
        if (record == null) {
            return null;
        }
        UserExamRecordVo recordVo = userTestMapper.selectUserTestDetail(recordId, userId);
        List<QuestionVo> questions = userTestMapper.selectQuestionsByExamId(record.getExam_id());
        List<Object> userAnswers = parseAnswerJson(record.getAnswer_json());

        int correctCount = 0;
        int wrongCount = 0;
        for (int i = 0; i < questions.size(); i++) {
            QuestionVo question = questions.get(i);
            Object userAnswer = i < userAnswers.size() ? userAnswers.get(i) : null;
            question.setUser_value(userAnswer);
            if (isSubjectiveQuestion(question)) {
                question.setIs_correct(null);
                question.setGained_score(null);
                continue;
            }
            boolean correct = isAnswerCorrect(question, userAnswer);
            question.setIs_correct(correct);
            question.setGained_score(correct ? safeScore(question) : 0);
            if (correct) {
                correctCount++;
            } else {
                wrongCount++;
            }
        }

        UserExamResultVo result = new UserExamResultVo();
        result.setId(record.getId());
        result.setAnswer_status(record.getAnswer_status());
        result.setRead_status(record.getRead_status());
        result.setScore(record.getScore());
        result.setCorrect_count(correctCount);
        result.setWrong_count(wrongCount);
        result.setQuestions(questions);
        if (recordVo != null) {
            result.setTestpaper(recordVo.getTestpaper());
            if (recordVo.getTestpaper() != null && recordVo.getTestpaper().getPass_score() != null) {
                result.setPassed(record.getScore() != null && record.getScore() >= recordVo.getTestpaper().getPass_score());
            }
        }
        return result;
    }

    private List<Object> parseAnswerJson(String answerJson) {
        if (answerJson == null || answerJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(answerJson, Object.class);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private boolean isAnswerCorrect(QuestionVo question, Object userAnswer) {
        if (userAnswer == null || question.getCorrect_answer() == null || isSubjectiveQuestion(question)) {
            return false;
        }
        String userValue = normalizeAnswer(JSON.toJSONString(userAnswer));
        String correctValue = normalizeAnswer(String.valueOf(question.getCorrect_answer()));
        return userValue.equals(correctValue);
    }

    private boolean isSubjectiveQuestion(QuestionVo question) {
        return question != null && "answer".equals(question.getType());
    }

    private String normalizeAnswer(String value) {
        if (value == null) {
            return "";
        }
        String text = value.trim().replace(" ", "");
        if (text.startsWith("[") && text.endsWith("]")) {
            try {
                JSONArray array = JSON.parseArray(text);
                List<String> values = new ArrayList<>();
                for (Object item : array) {
                    values.add(String.valueOf(item).replace("\"", "").trim());
                }
                Collections.sort(values);
                return String.join(",", values);
            } catch (Exception ignored) {
                return text;
            }
        }
        return text.replace("\"", "");
    }

    private int safeScore(QuestionVo question) {
        return question.getScore() == null ? 0 : question.getScore();
    }


    @Autowired
    private AiUtil aiUtil = new AiUtil();

    @Value("${deepseek.api.url:https://api.deepseek.com/chat/completions}")
    private String deepseekApiUrl;

    @Value("${deepseek.api.key:}")
    private String deepseekApiKey;

    @Value("${deepseek.api.model:deepseek-chat}")
    private String deepseekModel;
    /**
     * 封装业务 Prompt 并调用通用 AiUtil
     */
    private String callAiForScoring(String essayContent) {
        // 1. 构造业务指令
        String fullPrompt = "你是一位专业的数字人面试官。请针对以下简答题，结合参考答案和用户回答进行打分。改成你是个严格老师 每一分绝不乱给 不要轻易给满分 要结合参考答案 并且带上自己的判断 参考答案只是参考的\n" +
                "要求：\n" +
                "1. 评分要严谨，如果回答不着边际给0分。\n" +
                "2. 必须严格以JSON格式输出，不要包含任何解释文字。\n" +
                "3. 格式如下：{\"scores\": [分数1, 分数2], \"evaluation\": \"整体评价\"}\n\n" +
                "简答题内容如下：\n" + essayContent;

        if (deepseekApiKey == null || deepseekApiKey.trim().isEmpty()) {
            return null;
        }
        return aiUtil.sendRequest(deepseekApiUrl, deepseekApiKey, deepseekModel, fullPrompt);
    }
}
