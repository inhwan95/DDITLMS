package com.example.dditlms.domain.repository.sanctn;

import com.example.dditlms.domain.dto.QSanctnDTO;
import com.example.dditlms.domain.dto.SanctnDTO;
import com.example.dditlms.domain.entity.Member;
import com.example.dditlms.domain.entity.sanction.SanctnLn;
import com.example.dditlms.domain.entity.sanction.SanctnLnProgress;
import com.example.dditlms.domain.entity.sanction.SanctnProgress;
import com.example.dditlms.domain.repository.MemberRepository;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static com.example.dditlms.domain.entity.QDepartment.department;
import static com.example.dditlms.domain.entity.QEmployee.employee;
import static com.example.dditlms.domain.entity.QMember.member;
import static com.example.dditlms.domain.entity.sanction.QSanctn.sanctn;
import static com.example.dditlms.domain.entity.sanction.QSanctnLn.sanctnLn1;

@Slf4j
public class SanctnLnRepositoryImpl implements SanctnLnRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final MemberRepository memberRepository;


    public SanctnLnRepositoryImpl(EntityManager entityManager, MemberRepository memberRepository) {
        this.queryFactory = new JPAQueryFactory(entityManager);
        this.memberRepository = memberRepository;
    }


    // 결재라인의견 조회

    @Override
    public List<SanctnDTO> showSanctnLine2(Long id) {

        return queryFactory
                .select(new QSanctnDTO(sanctnLn1.sanctnDate
                        , sanctnLn1.sanctnOpinion
                        , sanctnLn1.lastApproval
                        , member.name
                        , employee.employeeRole
                        , department.deptNm
                        , sanctnLn1.sanctnStep
                        , sanctnLn1.sanctnLnProgress
                        , sanctnLn1.mberNo.userNumber))
                .from(sanctnLn1,
                        member,
                        sanctn,
                        employee,
                        department)
                .join(sanctnLn1.sanctnSn, sanctn)
                .join(sanctnLn1.mberNo, member)
                .join(employee.member, member)
                .join(employee.deptCode, department)
                .where(sanctnLn1.sanctnSn.eq(sanctn)
                        , sanctnLn1.mberNo.eq(member)
                        , sanctn.sanctnId.eq(id)
                        , employee.member.eq(member)
                        , employee.deptCode.eq(department))
                .groupBy(member.name)
                .groupBy(sanctnLn1.sanctnDate)
                .groupBy(sanctnLn1.sanctnOpinion)
                .groupBy(sanctnLn1.lastApproval)
                .groupBy(employee.employeeRole)
                .groupBy(department.deptNm)
                .groupBy(sanctnLn1.sanctnStep)
                .groupBy(sanctnLn1.sanctnLnProgress)
                .groupBy(sanctnLn1.mberNo.userNumber)
                .orderBy(sanctnLn1.sanctnStep.asc())
                .fetch();

    }

    @Override
    public SanctnLn findSanctnId(Long userNumber, Long id) {

        return queryFactory
                .select(sanctnLn1)
                .from(sanctnLn1)
                .where(sanctnLn1.mberNo.userNumber.eq(userNumber)
                        , sanctnLn1.sanctnSn.sanctnId.eq(id))
                .fetchOne();

    }

    @Override
    public SanctnLn findNextSanctnId(Long userNumber, Long id) {

        return queryFactory
                .selectFrom(sanctnLn1)
                .where(sanctnLn1.sanctnSn.sanctnId.eq(id)
                        , sanctnLn1.sanctnLnProgress.eq(SanctnLnProgress.WAITING))
                .orderBy(sanctnLn1.sanctnStep.asc())
                .fetchFirst();

    }
    
    
    // 최근 결재의견 조회
    @Override
    public List<SanctnDTO> findRecentOpinion(Long userNumber) {

        return queryFactory
                .select(new QSanctnDTO(sanctnLn1.sanctnDate
                        , sanctnLn1.sanctnOpinion
                        , sanctnLn1.lastApproval
                        , member.name
                        , employee.employeeRole
                        , department.deptNm
                        , sanctnLn1.sanctnStep
                        , sanctnLn1.sanctnLnProgress
                        , sanctnLn1.mberNo.userNumber))
                .from(sanctn
                        ,sanctnLn1
                        , member
                        , employee
                        , department)
                .join(sanctnLn1.sanctnSn, sanctn)
                .where(sanctnLn1.mberNo.userNumber.notIn(userNumber))
                .groupBy(sanctnLn1.sanctnDate)
                .groupBy(sanctnLn1.sanctnOpinion)
                .groupBy(member.name)
                .groupBy(employee.employeeRole)
                .groupBy(department.deptNm)
                .groupBy(sanctnLn1.sanctnStep)
                .groupBy(sanctnLn1.sanctnLnProgress)
                .groupBy(sanctnLn1.mberNo.userNumber)
                .join(sanctnLn1.mberNo, member)
                .join(employee.member, member)
                .join(employee.deptCode, department)
                .where(sanctnLn1.mberNo.eq(member)
                        , employee.member.eq(member)
                        , employee.deptCode.eq(department),
                        sanctnLn1.sanctnOpinion.isNotNull())
                .orderBy(sanctnLn1.sanctnDate.desc())
                .limit(5)
                .fetch();

    }

    // 결재 조회 (페이징 + 상태조건별 조회)
    @Override
    public Page<SanctnLn> inquirePageWithProgress(Long userNumber, Pageable pageable, SanctnProgress sanctnProgress) {

        Optional<Member> findMember = memberRepository.findByUserNumber(userNumber);


        QueryResults<SanctnLn> results = queryFactory
                .select(sanctnLn1)
                .from(sanctnLn1)
                .from(sanctn)
                .join(sanctnLn1.sanctnSn, sanctn)
                .where(sanctnLn1.sanctnSn.eq(sanctn)
                        , sanctnLn1.mberNo.eq(findMember.get())
                        , sanctn.status.eq(sanctnProgress))
                .groupBy(sanctnLn1.sanctnSn)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();
        List<SanctnLn> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    // 결재 전체 조회
   @Override
    public Page<SanctnLn> inquireAll(Long userNumber, Pageable pageable) {
       Optional<Member> findMember = memberRepository.findByUserNumber(userNumber);

       List<SanctnLn> content = queryFactory
               .select(sanctnLn1)
               .from(sanctnLn1)
               .from(sanctn)
               .join(sanctnLn1.sanctnSn, sanctn)
               .where(sanctnLn1.sanctnSn.eq(sanctn)
                       , sanctnLn1.mberNo.eq(findMember.get()))
               .groupBy(sanctnLn1.sanctnSn)
               .offset(pageable.getOffset())
               .limit(pageable.getPageSize())
               .fetch();

       JPAQuery<SanctnLn> countQuery = queryFactory
               .select(sanctnLn1)
               .from(sanctnLn1)
               .from(sanctn)
               .join(sanctnLn1.sanctnSn, sanctn)
               .where(sanctnLn1.sanctnSn.eq(sanctn), sanctnLn1.mberNo.eq(findMember.get()))
               .groupBy(sanctnLn1.sanctnSn);

       countQuery.fetchCount();

       return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
   }

}







