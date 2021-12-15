package com.example.dditlms.domain.dto;

import com.example.dditlms.domain.entity.sanction.EmployeeRole;
import com.example.dditlms.domain.entity.sanction.SanctnLnProgress;
import com.example.dditlms.domain.entity.sanction.SanctnProgress;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@ToString
public class SanctnDTO {
    private SanctnLnProgress sanctnLnProgress;
    private LocalDateTime sanctnDate;
    private String sanctnOpinion;
    private Integer sanctnStep;
    private String lastApproval;

    private String sanctnSj;
    private String sanctnCn;
    private LocalDate sanctnWritngde;
    private SanctnProgress status;
    private String name;
    private EmployeeRole employeeRole;
    private String deptNm;
    private Long userNumber;

    private Long countPro;


    public SanctnDTO() { }

    @QueryProjection

    public SanctnDTO(LocalDateTime sanctnDate, String sanctnOpinion, String lastApproval, String name, EmployeeRole employeeRole, String deptNm, Integer sanctnStep, SanctnLnProgress sanctnLnProgress, Long userNumber) {
        this.sanctnDate = sanctnDate;
        this.sanctnOpinion = sanctnOpinion;
        this.lastApproval = lastApproval;
        this.name = name;
        this.employeeRole = employeeRole;
        this.deptNm = deptNm;
        this.sanctnStep= sanctnStep;
        this.sanctnLnProgress = sanctnLnProgress;
        this.userNumber = userNumber;
    }

    @QueryProjection

    public SanctnDTO(Integer sanctnStep, SanctnLnProgress sanctnLnProgress) {
        this.sanctnStep = sanctnStep;
        this.sanctnLnProgress = sanctnLnProgress;
    }
}
