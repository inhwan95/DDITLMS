package com.example.dditlms.controller;

import com.example.dditlms.domain.common.Role;
import com.example.dditlms.domain.dto.DocFormDTO;
import com.example.dditlms.domain.dto.EmployeeDTO;
import com.example.dditlms.domain.dto.PageDTO;
import com.example.dditlms.domain.dto.SanctnDTO;
import com.example.dditlms.domain.entity.Bookmark;
import com.example.dditlms.domain.entity.Department;
import com.example.dditlms.domain.entity.Member;
import com.example.dditlms.domain.entity.sanction.*;
import com.example.dditlms.domain.repository.MemberRepository;
import com.example.dditlms.domain.repository.sanctn.*;
import com.example.dditlms.security.AccountContext;
import com.example.dditlms.service.SanctnLnService;
import com.example.dditlms.service.SanctnService;
import com.example.dditlms.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.view.RedirectView;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SanctnController {

    private final SanctnLnRepository sanctnLnRepository;

    private final MemberRepository memberRepository;

    private final EmployeeRepository employeeRepository;

    private final DocformRepository docformRepository;

    private final DepartmentRepository departmentRepository;

    private final SanctnService sanctnService;

    private final SanctnRepository sanctnRepository;

    private final SanctnLnService sanctnLnService;

    private final FileUtil fileUtil;

    //결재메인페이지 접속 시, 기본정보 출력용(단순 조회, 전체 숫자 & 진행정보만 출력)
    @GetMapping("/sanctn")
    public String santn(Model model, @PageableDefault(size = 8) Pageable pageable, SanctnProgress sanctnProgress) {

        //현재 로그인한 사용자 정보(userNumber)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber = member.getUserNumber();


        SanctnProgress reject = SanctnProgress.REJECT;
        Page<SanctnDTO> rejectResult = sanctnLnRepository.inquirePageWithProgress(userNumber, pageable, reject);

        long totalRej = rejectResult.getTotalElements();

        model.addAttribute("totalRej", totalRej);

        SanctnProgress pub = SanctnProgress.PUBLICIZE;
        Page<SanctnDTO> pubResult = sanctnLnRepository.inquirePageWithProgress(userNumber, pageable, pub);
        long totalPub = pubResult.getTotalElements();
        model.addAttribute("totalPub", totalPub);

        //전체 조회결과와 페이징 정보를 넘겨준다.

        SanctnProgress progress = SanctnProgress.PROGRESS;
        Page<SanctnDTO> results = sanctnLnRepository.inquirePageWithProgress(userNumber, pageable, progress);


        model.addAttribute("results", results);
        model.addAttribute("page", new PageDTO(results.getTotalElements(), pageable));
        long totalPro = results.getTotalElements();
        model.addAttribute("totalPro", totalPro);


        //로그인 한 사람 이름 조회, 넘기기
        Optional<Member> findUser = memberRepository.findByUserNumber(userNumber);
        String findname = findUser.get().getName();
        model.addAttribute("findname", findname);

        // 페이징 페이지 주소 매핑
        model.addAttribute("mapping", "sanctnRe");


        //최근결재의견 조회
        List<SanctnDTO> recentOpinion = sanctnLnRepository.findRecentOpinion(userNumber);
        model.addAttribute("recentOpinions" , recentOpinion);

        for (SanctnDTO sanctnDTO : recentOpinion) {
            Long sanctnId = sanctnDTO.getSanctnId();
            List<SanctnDTO> sanctnDTOS = sanctnLnRepository.countSanctn(sanctnId);
            int size = sanctnDTOS.size();
            sanctnDTO.getStatus();
        }


        return "/pages/sanction";
    }

    //에이잭스용 페이징 갱신 요청 주소
    @GetMapping("/sanctnRe")
    public String sanctnRe(Model model, @PageableDefault(size = 8) Pageable pageable, SanctnProgress sanctnProgress) {

        //현재 로그인한 사용자 정보(userNumber)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber = member.getUserNumber();

        SanctnProgress progress = SanctnProgress.PROGRESS;
        Page<SanctnDTO> results = sanctnLnRepository.inquirePageWithProgress(userNumber, pageable, progress);

        model.addAttribute("results", results);
        model.addAttribute("page", new PageDTO(results.getTotalElements(), pageable));

        // 페이징 페이지 주소 매핑
        model.addAttribute("mapping", "sanctnRe");

        return "/pages/sanction::#test";
    }


    // 기안하기 페이지

    @GetMapping("/drafting")
    public String drafting(Model model, SanctnForm sanctnForm) {
        //로그인한 사람 정보 조회 및 데이터 넘김
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber = member.getUserNumber();
        model.addAttribute("drafter", userNumber);

        // 로그인한 사람 이름 얻기
        Optional<Member> findUser = memberRepository.findByUserNumber(userNumber);
        String findname = findUser.get().getName();
        model.addAttribute("findname", findname);

        //로그인한 사람 직원 상세정보 조회
        List<EmployeeDTO> dtoList = employeeRepository.viewDetails(userNumber);
        EmployeeDTO empDetails = dtoList.get(0);
        model.addAttribute("empDetails", empDetails);

        //문서양식 전부 가져옴
        List<DocFormCategory> allDocFormList = docformRepository.allDocFormList();
        model.addAttribute("allDocFormList", allDocFormList);

        //부서 목록 전체 조회
        List<Department> departmentList = departmentRepository.findAll();
        model.addAttribute("departmentList", departmentList);


        return "/pages/drafting";
    }

    //양식폼 2차 카테고리 결과 반환
    @GetMapping("/sendFormCate")
    @ResponseBody
    public List<DocFormDTO> sendFormCate(@RequestParam Map<String, Object> param) {

        List<DocFormDTO> result = docformRepository.DocFromCate((String) param.get("cate"));

        return result;
    }

    //부서별 직원목록 반환
    @GetMapping("/sendDept")
    @ResponseBody
    public List<EmployeeDTO> sendDept(@RequestParam Map<String, Object> param) {

        List<EmployeeDTO> empList = employeeRepository.empList(Long.valueOf((String) param.get("dep")));

        return empList;
    }

    //양식폼 생성
    @GetMapping("/makeForm")
    @ResponseBody
    public Optional<Docform> makeForm(@RequestParam Map<Long, Object> param) {

        Optional<Docform> form = docformRepository.findById(Long.valueOf((String) param.get("form")));

        return form;
    }

    //직원 상세정보 조회
    @GetMapping("/viewDetails")
    @ResponseBody
    public List<EmployeeDTO> viewDetails(@RequestParam Map<String, Object> param) {

        List<EmployeeDTO> viewDetails = employeeRepository.viewDetails(Long.valueOf((String) param.get("userNumber")));

        return viewDetails;
    }

    //기안하기
    @PostMapping("/sanctnSubmit")
    public RedirectView submitSanctn(SanctnForm sanctnForm, @RequestParam(value = "file", required = false) MultipartFile file, MultipartHttpServletRequest request) {
        log.info("멀티파트 체킹!" + String.valueOf(request));

        long id = fileUtil.uploadFiles(request.getFileMap());

        sanctnService.saveSanctn(sanctnForm.getSanctnSj()
                , sanctnForm.getDocformSn()
                , sanctnForm.getDrafter()
                , sanctnForm.getSanctnCn()
                , sanctnForm.getUserNumber()
                , id);


        return new RedirectView("/sanctn");
    }


    //결재별 상세조회
    @GetMapping("/showSanctn/{id}")
    public String sanctnDetail(@PathVariable("id") Long id, Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber = member.getUserNumber();


        //로그인한 사람의 정보를 넘겨 줌
        model.addAttribute("userNumber", userNumber);

        Optional<Sanctn> details = sanctnRepository.findById(id);
        Sanctn sanctn = details.get();
        model.addAttribute("details", sanctn);

        Long drafter = details.get().getDrafter();
        Member findDrafter = memberRepository.findByUserNumber(drafter).get();
        Role role = findDrafter.getRole();

        if(role == Role.ROLE_STUDENT) {
            String compliment = "민원신청";
            model.addAttribute("drafterType" , compliment);
        }

        List<SanctnDTO> sanctnDTOS = sanctnLnRepository.showSanctnLine2(id);
        Map<String, Object> resultList = null;
        Optional<Map<String, Object>> result = Optional.ofNullable(sanctnLnRepository.viewCompliment(id));

        if (resultList != null) {
            resultList = result.get();
            Timestamp beforeConvertDate = (Timestamp) resultList.get("SANCTN_DATE");
            LocalDateTime localDateTime = beforeConvertDate.toLocalDateTime();
            Integer sanctn_step = Integer.valueOf(resultList.get("SANCTN_STEP").toString());
            String sanctn_ls_apv = resultList.get("SANCTN_LS_APV").toString();
            String mber_nm = resultList.get("MBER_NM").toString();
            Long mber_no = Long.valueOf(resultList.get("MBER_NO").toString());
            String major_nm_kr = resultList.get("MAJOR_NM_KR").toString();
            SanctnDTO sanctnDTO = new SanctnDTO();
            sanctnDTO.setSanctnDate(localDateTime);
            sanctnDTO.setSanctnStep(sanctn_step);
            sanctnDTO.setLastApproval(sanctn_ls_apv);
            sanctnDTO.setStatus(SanctnProgress.PROGRESS);
            sanctnDTO.setName(mber_nm);
            sanctnDTO.setUserNumber(mber_no);
            sanctnDTO.setMajor_nm_kr(major_nm_kr);
             //민원 신청 내역
            model.addAttribute("compliment", sanctnDTO);
            log.info(String.valueOf(sanctnDTO));
        }


        //일반 결재 내역
        model.addAttribute("sanctnLnList", sanctnDTOS);

        //문서 ID 넘겨줌
        model.addAttribute("id", id);

        return "/pages/sanctionDetail";
    }

    // 결재 승인하기
    @PostMapping("/approval")
    public String apropval(@RequestParam Map<String, Object> param, Model model) {

        //의견 남기기 + 결재승인 처리
        Object opinion = param.get("opinion");
        Long userNumber = Long.valueOf((String) param.get("userNumber"));
        Long id = Long.valueOf((String) param.get("id"));

        sanctnLnService.updateSanctnLn(opinion.toString(), userNumber, id);

        //결재별 상세조회 페이지와 동일한 메소드, 리팩토링 필요

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber2 = member.getUserNumber();


        //로그인한 사람의 정보를 넘겨 줌
        model.addAttribute("userNumber", userNumber2);

        Optional<Sanctn> details = sanctnRepository.findById(id);
        Sanctn sanctn = details.get();
        model.addAttribute("details", sanctn);

        List<SanctnDTO> sanctnDTOS = sanctnLnRepository.showSanctnLine2(id);

        model.addAttribute("sanctnLnList", sanctnDTOS);

        //문서 ID 넘겨줌
        model.addAttribute("id", id);

        return "/pages/sanctionDetail";

    }

    //결재 반려처리
    @PostMapping("/reject")
    public String reject(@RequestParam Map<String, Object> param, Model model) {


        Object opinion = param.get("opinion");
        Long userNumber = Long.valueOf((String) param.get("userNumber"));
        Long id = Long.valueOf((String) param.get("id"));

        sanctnLnService.rejectSanctnLn(opinion.toString(), userNumber, id);

        //결재별 상세조회 페이지와 동일한 메소드, 리팩토링 필요

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber2 = member.getUserNumber();


        //로그인한 사람의 정보를 넘겨 줌
        model.addAttribute("userNumber", userNumber2);

        Optional<Sanctn> details = sanctnRepository.findById(id);
        Sanctn sanctn = details.get();
        model.addAttribute("details", sanctn);

        List<SanctnDTO> sanctnDTOS = sanctnLnRepository.showSanctnLine2(id);

        model.addAttribute("sanctnLnList", sanctnDTOS);

        //문서 ID 넘겨줌
        model.addAttribute("id", id);

        return "/pages/sanctionDetail";
    }


    //최종승인
    @PostMapping("/finalApproval")
    public String finalApproval(@RequestParam Map<String, Object> param, Model model) {


        Object opinion = param.get("opinion");
        Long userNumber = Long.valueOf((String) param.get("userNumber"));
        Long id = Long.valueOf((String) param.get("id"));

        sanctnLnService.lastUpadteSanctnLn(opinion.toString(), userNumber, id);

        //결재별 상세조회 페이지와 동일한 메소드, 리팩토링 필요

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber2 = member.getUserNumber();


        //로그인한 사람의 정보를 넘겨 줌
        model.addAttribute("userNumber", userNumber2);

        Optional<Sanctn> details = sanctnRepository.findById(id);
        Sanctn sanctn = details.get();
        model.addAttribute("details", sanctn);

        List<SanctnDTO> sanctnDTOS = sanctnLnRepository.showSanctnLine2(id);

        model.addAttribute("sanctnLnList", sanctnDTOS);

        //문서 ID 넘겨줌
        model.addAttribute("id", id);

        return "/pages/sanctionDetail";
    }

    //진행 조회
    @GetMapping("/sanctnProgress")
    public String sanctnProgress(Model model, @PageableDefault(size = 8) Pageable pageable) {
        //현재 로그인한 사용자 정보(userNumber)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber = member.getUserNumber();

        SanctnProgress progress = SanctnProgress.PROGRESS;

        Page<SanctnDTO> results = sanctnLnRepository.inquirePageWithProgress(userNumber, pageable, progress);

        model.addAttribute("results", results);
        model.addAttribute("page", new PageDTO(results.getTotalElements(), pageable));

        // 페이징 페이지 주소 매핑
        model.addAttribute("mapping", "sanctnProgress");

        return "/pages/sanction::#test";

    }

    //반려 조회
    @GetMapping("/sanctnReject")
    public String sanctnReject(Model model, @PageableDefault(size = 8) Pageable pageable) {

        //현재 로그인한 사용자 정보(userNumber)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber = member.getUserNumber();

        SanctnProgress reject = SanctnProgress.REJECT;
        Page<SanctnDTO> results = sanctnLnRepository.inquirePageWithProgress(userNumber, pageable, reject);

        model.addAttribute("results", results);
        model.addAttribute("page", new PageDTO(results.getTotalElements(), pageable));


        // 페이징 페이지 주소 매핑
        model.addAttribute("mapping", "sanctnReject");

        return "/pages/sanction::#test";
    }

    //공람 조회

    @GetMapping("/sanctnPublic")
    public String sanctnPublic(Model model, @PageableDefault(size = 8) Pageable pageable) {

        //현재 로그인한 사용자 정보(userNumber)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber = member.getUserNumber();

        SanctnProgress publicize = SanctnProgress.PUBLICIZE;
        Page<SanctnDTO> results = sanctnLnRepository.inquirePageWithProgress(userNumber, pageable, publicize);

        model.addAttribute("results", results);
        model.addAttribute("page", new PageDTO(results.getTotalElements(), pageable));

        // 페이징 페이지 주소 매핑
        model.addAttribute("mapping", "sanctnPublic");

        return "/pages/sanction::#test";

    }

    // 전체 조회

    @GetMapping("/sanctnAll")
    public String sanctnAll(Model model, @PageableDefault(size = 8) Pageable pageable) {
        //현재 로그인한 사용자 정보(userNumber)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber = member.getUserNumber();

        Page<SanctnDTO> results = sanctnLnRepository.inquireAll(userNumber, pageable);

        model.addAttribute("results", results);
        model.addAttribute("page", new PageDTO(results.getTotalElements(), pageable));

        // 페이징 페이지 주소 매핑
        model.addAttribute("mapping", "sanctnAll");


        return "pages/sanction::#test";

    }
    
    // 완료 조회
    @GetMapping("/sanctnCom")
    public String sanctnCom(Model model, @PageableDefault(size = 8) Pageable pageable) {

        //현재 로그인한 사용자 정보(userNumber)를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        try {
            member = ((AccountContext) authentication.getPrincipal()).getMember();
        } catch (ClassCastException e) {
        }
        Long userNumber = member.getUserNumber();

        SanctnProgress completion = SanctnProgress.COMPLETION;
        Page<SanctnDTO> results = sanctnLnRepository.inquirePageWithProgress(userNumber, pageable, completion);

        model.addAttribute("results", results);
        model.addAttribute("page", new PageDTO(results.getTotalElements(), pageable));

        return "/pages/sanction::#test";

    }

}