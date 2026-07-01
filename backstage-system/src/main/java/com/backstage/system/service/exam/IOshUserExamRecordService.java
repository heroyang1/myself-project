package com.backstage.system.service.exam;

import com.backstage.system.domain.dto.exam.UserExamSaveDto;
import com.backstage.system.domain.vo.exam.UserExamRecordVo;
import com.backstage.system.domain.vo.exam.UserExamResultVo;
import com.backstage.system.domain.vo.exam.UserExamSaveVo;

import java.util.List;

public interface IOshUserExamRecordService {
    List<UserExamRecordVo> selectUserTestList(Integer page);

    boolean saveUserExam(UserExamSaveDto saveDto);

    UserExamResultVo getUserExamResult(Long recordId);
}