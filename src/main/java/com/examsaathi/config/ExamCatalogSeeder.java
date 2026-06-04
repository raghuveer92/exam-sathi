package com.examsaathi.config;

import com.examsaathi.entity.Exam;
import com.examsaathi.entity.ExamCategory;
import com.examsaathi.repository.ExamCategoryRepository;
import com.examsaathi.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class ExamCatalogSeeder implements CommandLineRunner {

    private final ExamCategoryRepository categoryRepository;
    private final ExamRepository examRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) return;
        seedCategoriesAndSampleExams();
    }

    private void seedCategoriesAndSampleExams() {
        Map<String, String[]> categories = new LinkedHashMap<>();
        categories.put("Government Exams", new String[]{"account_balance", "SSC, Banking, Railway, UPSC and state exams"});
        categories.put("Engineering Exams", new String[]{"engineering", "JEE Main, JEE Advanced and more"});
        categories.put("Medical Exams", new String[]{"medical_services", "NEET and medical entrance exams"});
        categories.put("School Exams", new String[]{"school", "CBSE, State Board, RBSE and school boards"});
        categories.put("Other Exams", new String[]{"category", "CUET, NDA, Defence and more"});

        int order = 1;
        ExamCategory government = null;
        for (var entry : categories.entrySet()) {
            ExamCategory cat = categoryRepository.save(ExamCategory.builder()
                .name(entry.getKey())
                .description(entry.getValue()[1])
                .icon(entry.getValue()[0])
                .displayOrder(order++)
                .isActive(true)
                .build());
            if ("Government Exams".equals(entry.getKey())) government = cat;
        }

        seedExam(government, "SSC CGL", "SSC", "Staff Selection Commission", "Popular government exam", true, true, 1);
        seedExam(government, "Banking", "BANK", "Banking exams", "IBPS, SBI and more", true, false, 2);
        seedExam(government, "Railway NTPC", "RRB", "Railway recruitment", "Railway NTPC and group exams", true, true, 3);
        seedExam(government, "UPSC", "UPSC", "Civil Services", "IAS, IPS preparation", false, true, 4);
        seedExam(government, "State PSC", "PSC", "State public service", "State-level competitive exams", false, false, 5);
        seedExam(government, "REET", "REET", "Rajasthan Teacher Eligibility", "REET Level 1 & 2", true, true, 6);

        ExamCategory engineering = categoryRepository.findAll().stream()
            .filter(c -> "Engineering Exams".equals(c.getName())).findFirst().orElse(null);
        seedExam(engineering, "JEE Main", "JEE_MAIN", "Engineering entrance", "National engineering exam", true, true, 1);
        seedExam(engineering, "JEE Advanced", "JEE_ADV", "Advanced engineering", "IIT entrance", false, false, 2);

        ExamCategory medical = categoryRepository.findAll().stream()
            .filter(c -> "Medical Exams".equals(c.getName())).findFirst().orElse(null);
        seedExam(medical, "NEET", "NEET", "Medical entrance", "National eligibility cum entrance test", true, true, 1);

        ExamCategory school = categoryRepository.findAll().stream()
            .filter(c -> "School Exams".equals(c.getName())).findFirst().orElse(null);
        assignExistingExam(school, "10th (CBSE)", "CBSE10", "Classes 6 - 12", true, 1);
        seedExam(school, "State Board", "STATE", "State board syllabus", "Classes 6 - 12", true, true, 2);
        seedExam(school, "RBSE", "RBSE", "Rajasthan Board", "Rajasthan state board", false, false, 3);

        ExamCategory other = categoryRepository.findAll().stream()
            .filter(c -> "Other Exams".equals(c.getName())).findFirst().orElse(null);
        seedExam(other, "CUET", "CUET", "University entrance", "Common University Entrance Test", true, false, 1);
        seedExam(other, "NDA", "NDA", "Defence academy", "National Defence Academy", false, true, 2);

        log.info("Seeded exam categories and catalog exams");
    }

    private void assignExistingExam(ExamCategory category, String name, String code,
                                    String shortDesc, boolean popular, int displayOrder) {
        examRepository.findByNameIgnoreCase(name).ifPresent(exam -> {
            exam.setCategory(category);
            exam.setShortDescription(shortDesc);
            exam.setPopular(popular);
            exam.setDisplayOrder(displayOrder);
            if (exam.getCode() == null) exam.setCode(code);
            examRepository.save(exam);
        });
    }

    private void seedExam(ExamCategory category, String name, String code, String shortDesc,
                          String description, boolean featured, boolean popular, int displayOrder) {
        if (category == null || examRepository.findByNameIgnoreCase(name).isPresent()) return;
        examRepository.save(Exam.builder()
            .name(name)
            .code(code)
            .shortDescription(shortDesc)
            .description(description)
            .category(category)
            .colorCode("#6C63FF")
            .featured(featured)
            .popular(popular)
            .displayOrder(displayOrder)
            .featuredOrder(featured ? displayOrder : 0)
            .isActive(true)
            .build());
    }
}
