package com.examsaathi.config;

import com.examsaathi.entity.*;
import com.examsaathi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Seeds subjects, chapters, and topics for catalog exams that have no syllabus yet.
 */
@Component
@Order(4)
@RequiredArgsConstructor
@Slf4j
public class ExamSyllabusSeeder implements CommandLineRunner {

    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ChapterRepository chapterRepository;
    private final TopicRepository topicRepository;

    private static final Map<String, List<SubjectBlueprint>> SYLLABUS_BY_CODE = Map.ofEntries(
        Map.entry("SSC", sscCgl()),
        Map.entry("SSC_CGL", sscCgl()),
        Map.entry("BANK", banking()),
        Map.entry("RRB", railway()),
        Map.entry("UPSC", upsc()),
        Map.entry("PSC", statePsc()),
        Map.entry("REET", reet()),
        Map.entry("JEE_MAIN", jeeMain()),
        Map.entry("JEE_ADV", jeeAdvanced()),
        Map.entry("NEET", neet()),
        Map.entry("STATE", stateBoard()),
        Map.entry("RBSE", rbse()),
        Map.entry("CUET", cuet()),
        Map.entry("NDA", nda())
    );

    @Override
    @Transactional
    public void run(String... args) {
        int seeded = 0;
        for (Exam exam : examRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()) {
            if (exam.getCode() == null || examSubjectRepository.countByExamIdAndIsActiveTrue(exam.getId()) > 0) {
                continue;
            }
            List<SubjectBlueprint> blueprint = resolveBlueprint(exam);
            if (blueprint == null) {
                log.warn("No syllabus blueprint for exam: {} ({})", exam.getName(), exam.getCode());
                continue;
            }
            applyBlueprint(exam, blueprint);
            seeded++;
            log.info("Seeded syllabus for {} ({} subjects)", exam.getName(), blueprint.size());
        }
        if (seeded > 0) {
            log.info("Exam syllabus seeding complete: {} exams", seeded);
        }
    }

    private void applyBlueprint(Exam exam, List<SubjectBlueprint> subjects) {
        for (SubjectBlueprint sb : subjects) {
            String normalizedKey = normalize(exam.getCode() + " " + sb.name());
            Subject subject = subjectRepository.findByNormalizedName(normalizedKey)
                .orElseGet(() -> subjectRepository.save(Subject.builder()
                    .name(sb.name())
                    .normalizedName(normalizedKey)
                    .description(sb.description())
                    .iconName(sb.icon())
                    .colorCode(sb.color())
                    .isActive(true)
                    .build()));

            examSubjectRepository.findByExamIdAndSubjectId(exam.getId(), subject.getId())
                .orElseGet(() -> examSubjectRepository.save(ExamSubject.builder()
                    .exam(exam)
                    .subject(subject)
                    .displayOrder(sb.order())
                    .isActive(true)
                    .build()));

            for (ChapterBlueprint cb : sb.chapters()) {
                Chapter chapter = chapterRepository.save(Chapter.builder()
                    .title(cb.title())
                    .description(cb.description())
                    .orderIndex(cb.order())
                    .isActive(true)
                    .subject(subject)
                    .build());

                for (TopicBlueprint tb : cb.topics()) {
                    topicRepository.save(Topic.builder()
                        .title(tb.title())
                        .estimatedHours(tb.hours())
                        .difficultyLevel(tb.difficulty())
                        .orderIndex(tb.order())
                        .isActive(true)
                        .chapter(chapter)
                        .build());
                }
            }
        }
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private List<SubjectBlueprint> resolveBlueprint(Exam exam) {
        if (exam.getCode() != null) {
            List<SubjectBlueprint> byCode = SYLLABUS_BY_CODE.get(exam.getCode());
            if (byCode != null) return byCode;
        }
        return switch (exam.getName() == null ? "" : exam.getName().trim()) {
            case "SSC CGL" -> sscCgl();
            default -> null;
        };
    }

    // ── Blueprint builders ───────────────────────────────────────────────────

    private static List<SubjectBlueprint> sscCgl() {
        return List.of(
            subject("Quantitative Aptitude", "SSC CGL maths", "calculate", "#1565C0", 1, List.of(
                chapter("Arithmetic", "Number system and percentages", 1, List.of(
                    topic("Number System", 2, Topic.DifficultyLevel.EASY, 1),
                    topic("LCM & HCF", 1.5, Topic.DifficultyLevel.EASY, 2),
                    topic("Percentages", 2, Topic.DifficultyLevel.MEDIUM, 3),
                    topic("Ratio & Proportion", 2, Topic.DifficultyLevel.MEDIUM, 4),
                    topic("Profit & Loss", 2.5, Topic.DifficultyLevel.MEDIUM, 5)
                )),
                chapter("Algebra", "Equations and identities", 2, List.of(
                    topic("Linear Equations", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Quadratic Equations", 2.5, Topic.DifficultyLevel.HARD, 2),
                    topic("Algebraic Identities", 1.5, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Geometry & Mensuration", "2D and 3D figures", 3, List.of(
                    topic("Triangles & Circles", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Mensuration 2D", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Mensuration 3D", 2.5, Topic.DifficultyLevel.HARD, 3)
                ))
            )),
            subject("English Language", "SSC CGL English", "menu_book", "#2E7D32", 2, List.of(
                chapter("Grammar", "Core grammar rules", 1, List.of(
                    topic("Tenses", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Active & Passive Voice", 1.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Direct & Indirect Speech", 1.5, Topic.DifficultyLevel.MEDIUM, 3),
                    topic("Articles & Prepositions", 1.5, Topic.DifficultyLevel.EASY, 4)
                )),
                chapter("Vocabulary", "Word power", 2, List.of(
                    topic("Synonyms & Antonyms", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("One Word Substitution", 1.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Idioms & Phrases", 2, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Comprehension", "Reading skills", 3, List.of(
                    topic("Reading Comprehension", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Cloze Test", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Sentence Improvement", 2, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            )),
            subject("General Intelligence & Reasoning", "SSC reasoning", "psychology", "#6A1B9A", 3, List.of(
                chapter("Verbal Reasoning", "Logic puzzles", 1, List.of(
                    topic("Analogy", 1.5, Topic.DifficultyLevel.EASY, 1),
                    topic("Classification", 1.5, Topic.DifficultyLevel.EASY, 2),
                    topic("Series", 2, Topic.DifficultyLevel.MEDIUM, 3),
                    topic("Coding-Decoding", 2, Topic.DifficultyLevel.MEDIUM, 4)
                )),
                chapter("Non-Verbal Reasoning", "Visual patterns", 2, List.of(
                    topic("Figure Series", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Mirror & Water Image", 1.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Paper Folding", 1.5, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Analytical Reasoning", "Puzzles", 3, List.of(
                    topic("Syllogism", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Blood Relations", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Seating Arrangement", 3, Topic.DifficultyLevel.HARD, 3)
                ))
            )),
            subject("General Awareness", "SSC GK", "public", "#E65100", 4, List.of(
                chapter("History", "Indian & world history", 1, List.of(
                    topic("Ancient India", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Medieval India", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Modern India", 3, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Polity & Economy", "Governance basics", 2, List.of(
                    topic("Indian Constitution", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Indian Economy", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Government Schemes", 2, Topic.DifficultyLevel.EASY, 3)
                )),
                chapter("Science & Geography", "General science", 3, List.of(
                    topic("Physics Basics", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Chemistry Basics", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Indian Geography", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            ))
        );
    }

    private static List<SubjectBlueprint> banking() {
        return List.of(
            subject("Quantitative Aptitude", "Banking maths", "calculate", "#1565C0", 1, List.of(
                chapter("Data Interpretation", "Charts and tables", 1, List.of(
                    topic("Bar & Line Graphs", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Pie Charts", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Tables & Caselets", 3, Topic.DifficultyLevel.HARD, 3)
                )),
                chapter("Arithmetic", "Core calculations", 2, List.of(
                    topic("Simplification", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Number Series", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Quadratic Equations", 2, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            )),
            subject("Reasoning Ability", "Banking reasoning", "psychology", "#6A1B9A", 2, List.of(
                chapter("Puzzles", "Logical puzzles", 1, List.of(
                    topic("Floor & Box Puzzles", 3, Topic.DifficultyLevel.HARD, 1),
                    topic("Scheduling Puzzles", 2.5, Topic.DifficultyLevel.HARD, 2),
                    topic("Inequality", 2, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Verbal Reasoning", "Words and logic", 2, List.of(
                    topic("Syllogism", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Input-Output", 2.5, Topic.DifficultyLevel.HARD, 2),
                    topic("Coding-Decoding", 2, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            )),
            subject("English Language", "Banking English", "menu_book", "#2E7D32", 3, List.of(
                chapter("Grammar & Usage", "Rules and application", 1, List.of(
                    topic("Error Spotting", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Fill in the Blanks", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Para Jumbles", 2, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Reading", "Comprehension", 2, List.of(
                    topic("Reading Comprehension", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Cloze Test", 2, Topic.DifficultyLevel.MEDIUM, 2)
                ))
            )),
            subject("General Awareness", "Banking awareness", "account_balance", "#E65100", 4, List.of(
                chapter("Banking Knowledge", "Industry fundamentals", 1, List.of(
                    topic("RBI & Monetary Policy", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Banking Terms", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Financial Markets", 2, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Current Affairs", "Recent updates", 2, List.of(
                    topic("National Current Affairs", 3, Topic.DifficultyLevel.EASY, 1),
                    topic("Economy & Budget", 2.5, Topic.DifficultyLevel.MEDIUM, 2)
                ))
            ))
        );
    }

    private static List<SubjectBlueprint> railway() {
        return List.of(
            subject("Mathematics", "Railway maths", "calculate", "#1565C0", 1, List.of(
                chapter("Number System", "Basics", 1, List.of(
                    topic("BODMAS & Simplification", 2, Topic.DifficultyLevel.EASY, 1),
                    topic("Decimals & Fractions", 1.5, Topic.DifficultyLevel.EASY, 2),
                    topic("Average & Age Problems", 2, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Advanced Maths", "Higher topics", 2, List.of(
                    topic("Time & Work", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Time, Speed & Distance", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Trigonometry Basics", 2, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            )),
            subject("General Intelligence", "Railway reasoning", "psychology", "#6A1B9A", 2, List.of(
                chapter("Reasoning", "Patterns", 1, List.of(
                    topic("Analogy & Classification", 2, Topic.DifficultyLevel.EASY, 1),
                    topic("Series & Coding", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Matrix & Venn Diagram", 2, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Puzzles", "Applied logic", 2, List.of(
                    topic("Direction Sense", 1.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Blood Relations", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Seating Arrangement", 2.5, Topic.DifficultyLevel.HARD, 3)
                ))
            )),
            subject("General Awareness", "Railway GK", "train", "#E65100", 3, List.of(
                chapter("Static GK", "Core facts", 1, List.of(
                    topic("Indian History", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Geography", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Polity", 2, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Railway & Science", "Exam-specific", 2, List.of(
                    topic("Railway History & Zones", 2, Topic.DifficultyLevel.EASY, 1),
                    topic("General Science", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Current Affairs", 2, Topic.DifficultyLevel.EASY, 3)
                ))
            ))
        );
    }

    private static List<SubjectBlueprint> upsc() {
        return List.of(
            subject("History", "UPSC history", "history_edu", "#8D6E63", 1, List.of(
                chapter("Ancient & Medieval", "Early India", 1, List.of(
                    topic("Indus Valley Civilization", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Maurya & Gupta Empire", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Delhi Sultanate & Mughals", 3, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Modern India", "Freedom movement", 2, List.of(
                    topic("British Rule & Revolt 1857", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Congress & Freedom Struggle", 3, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Post-Independence India", 2, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            )),
            subject("Geography", "UPSC geography", "public", "#0277BD", 2, List.of(
                chapter("Physical Geography", "Earth systems", 1, List.of(
                    topic("Geomorphology", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Climatology", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Oceanography", 2, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Indian Geography", "India focus", 2, List.of(
                    topic("Physical Features of India", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Rivers & Climate", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Agriculture & Resources", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            )),
            subject("Indian Polity", "Constitution", "gavel", "#5E35B1", 3, List.of(
                chapter("Constitution", "Framework", 1, List.of(
                    topic("Preamble & Fundamental Rights", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Directive Principles", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Parliament & Judiciary", 3, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Governance", "Institutions", 2, List.of(
                    topic("Centre-State Relations", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Constitutional Bodies", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Local Government", 1.5, Topic.DifficultyLevel.EASY, 3)
                ))
            )),
            subject("Economy", "Indian economy", "trending_up", "#2E7D32", 4, List.of(
                chapter("Basic Concepts", "Macro fundamentals", 1, List.of(
                    topic("National Income", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Inflation & Monetary Policy", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Fiscal Policy & Budget", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Development", "Growth & planning", 2, List.of(
                    topic("Poverty & Unemployment", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Agriculture & Industry", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Banking & Finance", 2, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            )),
            subject("CSAT Aptitude", "UPSC aptitude", "calculate", "#1565C0", 5, List.of(
                chapter("Comprehension & Logic", "Paper 2 skills", 1, List.of(
                    topic("Reading Comprehension", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Logical Reasoning", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Data Interpretation", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            ))
        );
    }

    private static List<SubjectBlueprint> statePsc() {
        return List.of(
            subject("General Studies", "State PSC GS", "public", "#E65100", 1, List.of(
                chapter("State & National GK", "Regional focus", 1, List.of(
                    topic("State History & Culture", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Indian Polity Overview", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Current Affairs", 2.5, Topic.DifficultyLevel.EASY, 3)
                )),
                chapter("Aptitude", "Basic maths & reasoning", 2, List.of(
                    topic("Arithmetic", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Reasoning", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("English/Hindi Basics", 2, Topic.DifficultyLevel.EASY, 3)
                ))
            ))
        );
    }

    private static List<SubjectBlueprint> reet() {
        return List.of(
            subject("Child Development & Pedagogy", "REET CDP", "child_care", "#AD1457", 1, List.of(
                chapter("Learning & Development", "Child psychology", 1, List.of(
                    topic("Piaget & Kohlberg", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Learning Theories", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Inclusive Education", 2, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Teaching Methods", "Pedagogy", 2, List.of(
                    topic("Teaching-Learning Process", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Assessment & Evaluation", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Classroom Management", 1.5, Topic.DifficultyLevel.EASY, 3)
                ))
            )),
            subject("Language I (Hindi)", "REET Hindi", "translate", "#C62828", 2, List.of(
                chapter("Hindi Grammar", "Vyakaran", 1, List.of(
                    topic("Sandhi & Samas", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Alankar & Ras", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Comprehension", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            )),
            subject("Mathematics", "REET maths", "calculate", "#1565C0", 3, List.of(
                chapter("Arithmetic", "School maths", 1, List.of(
                    topic("Number System", 2, Topic.DifficultyLevel.EASY, 1),
                    topic("Percentage & Ratio", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Geometry", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            )),
            subject("Environmental Studies", "REET EVS", "eco", "#2E7D32", 4, List.of(
                chapter("EVS Concepts", "Environment", 1, List.of(
                    topic("Family & Food", 1.5, Topic.DifficultyLevel.EASY, 1),
                    topic("Water & Health", 2, Topic.DifficultyLevel.EASY, 2),
                    topic("Plants & Animals", 2, Topic.DifficultyLevel.EASY, 3)
                ))
            ))
        );
    }

    private static List<SubjectBlueprint> jeeMain() {
        return List.of(
            subject("Physics", "JEE Main physics", "science", "#5C6BC0", 1, List.of(
                chapter("Mechanics", "Classical mechanics", 1, List.of(
                    topic("Kinematics", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Laws of Motion", 3, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Work, Energy & Power", 3, Topic.DifficultyLevel.MEDIUM, 3),
                    topic("Rotational Motion", 3.5, Topic.DifficultyLevel.HARD, 4)
                )),
                chapter("Electricity & Magnetism", "EM topics", 2, List.of(
                    topic("Electrostatics", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Current Electricity", 3, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Magnetic Effects", 3, Topic.DifficultyLevel.HARD, 3)
                )),
                chapter("Modern Physics", "Quantum basics", 3, List.of(
                    topic("Dual Nature of Matter", 2.5, Topic.DifficultyLevel.HARD, 1),
                    topic("Atoms & Nuclei", 3, Topic.DifficultyLevel.HARD, 2)
                ))
            )),
            subject("Chemistry", "JEE Main chemistry", "biotech", "#00897B", 2, List.of(
                chapter("Physical Chemistry", "Numerical chemistry", 1, List.of(
                    topic("Mole Concept", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Atomic Structure", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Chemical Equilibrium", 3, Topic.DifficultyLevel.HARD, 3)
                )),
                chapter("Organic Chemistry", "Carbon compounds", 2, List.of(
                    topic("General Organic Chemistry", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Hydrocarbons", 3, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Functional Groups", 3.5, Topic.DifficultyLevel.HARD, 3)
                )),
                chapter("Inorganic Chemistry", "Periodic table", 3, List.of(
                    topic("Periodic Table", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Chemical Bonding", 3, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Coordination Compounds", 3, Topic.DifficultyLevel.HARD, 3)
                ))
            )),
            subject("Mathematics", "JEE Main maths", "calculate", "#1565C0", 3, List.of(
                chapter("Algebra", "Core algebra", 1, List.of(
                    topic("Complex Numbers", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Quadratic Equations", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Sequences & Series", 3, Topic.DifficultyLevel.HARD, 3)
                )),
                chapter("Calculus", "Limits & derivatives", 2, List.of(
                    topic("Limits & Continuity", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Differentiation", 3.5, Topic.DifficultyLevel.HARD, 2),
                    topic("Integration", 4, Topic.DifficultyLevel.HARD, 3)
                )),
                chapter("Coordinate Geometry", "2D geometry", 3, List.of(
                    topic("Straight Lines", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Circles", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Conic Sections", 3.5, Topic.DifficultyLevel.HARD, 3)
                ))
            ))
        );
    }

    private static List<SubjectBlueprint> jeeAdvanced() {
        return List.of(
            subject("Physics (Advanced)", "JEE Adv physics", "science", "#5C6BC0", 1, List.of(
                chapter("Advanced Mechanics", "Deep mechanics", 1, List.of(
                    topic("Rigid Body Dynamics", 4, Topic.DifficultyLevel.HARD, 1),
                    topic("Gravitation", 3, Topic.DifficultyLevel.HARD, 2),
                    topic("SHM & Waves", 4, Topic.DifficultyLevel.HARD, 3)
                )),
                chapter("Electrodynamics", "Advanced EM", 2, List.of(
                    topic("EM Induction & AC", 4, Topic.DifficultyLevel.HARD, 1),
                    topic("Optics", 3.5, Topic.DifficultyLevel.HARD, 2)
                ))
            )),
            subject("Chemistry (Advanced)", "JEE Adv chemistry", "biotech", "#00897B", 2, List.of(
                chapter("Advanced Organic", "Mechanisms", 1, List.of(
                    topic("Reaction Mechanisms", 4, Topic.DifficultyLevel.HARD, 1),
                    topic("Biomolecules", 3, Topic.DifficultyLevel.HARD, 2),
                    topic("Polymers", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Physical Chemistry Advanced", "Thermodynamics", 2, List.of(
                    topic("Thermodynamics", 4, Topic.DifficultyLevel.HARD, 1),
                    topic("Electrochemistry", 3.5, Topic.DifficultyLevel.HARD, 2)
                ))
            )),
            subject("Mathematics (Advanced)", "JEE Adv maths", "calculate", "#1565C0", 3, List.of(
                chapter("Advanced Calculus", "Analysis", 1, List.of(
                    topic("Definite Integration", 4, Topic.DifficultyLevel.HARD, 1),
                    topic("Differential Equations", 3.5, Topic.DifficultyLevel.HARD, 2),
                    topic("Area Under Curves", 3, Topic.DifficultyLevel.HARD, 3)
                )),
                chapter("Algebra & Vectors", "Higher algebra", 2, List.of(
                    topic("Matrices & Determinants", 3.5, Topic.DifficultyLevel.HARD, 1),
                    topic("3D Geometry", 4, Topic.DifficultyLevel.HARD, 2),
                    topic("Probability", 3, Topic.DifficultyLevel.HARD, 3)
                ))
            ))
        );
    }

    private static List<SubjectBlueprint> neet() {
        return List.of(
            subject("Physics", "NEET physics", "science", "#5C6BC0", 1, List.of(
                chapter("Mechanics", "Motion & forces", 1, List.of(
                    topic("Units & Measurements", 2, Topic.DifficultyLevel.EASY, 1),
                    topic("Motion in 1D & 2D", 3, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Laws of Motion", 3, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Thermodynamics & Optics", "Heat & light", 2, List.of(
                    topic("Thermodynamics", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Ray Optics", 3, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Wave Optics", 2.5, Topic.DifficultyLevel.HARD, 3)
                ))
            )),
            subject("Chemistry", "NEET chemistry", "biotech", "#00897B", 2, List.of(
                chapter("Physical Chemistry", "Numerical", 1, List.of(
                    topic("Structure of Atom", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Chemical Bonding", 3, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Equilibrium", 3, Topic.DifficultyLevel.HARD, 3)
                )),
                chapter("Organic Chemistry", "Biomolecules", 2, List.of(
                    topic("GOC", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Hydrocarbons", 3, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Biomolecules", 3, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            )),
            subject("Biology", "NEET biology", "biotech", "#43A047", 3, List.of(
                chapter("Botany", "Plant biology", 1, List.of(
                    topic("Plant Morphology", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Plant Physiology", 3, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Plant Reproduction", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Zoology", "Animal biology", 2, List.of(
                    topic("Human Physiology", 4, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Reproduction & Genetics", 4, Topic.DifficultyLevel.HARD, 2),
                    topic("Evolution & Ecology", 3, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Cell Biology", "Micro level", 3, List.of(
                    topic("Cell Structure", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Biotechnology", 2.5, Topic.DifficultyLevel.MEDIUM, 2)
                ))
            ))
        );
    }

    private static List<SubjectBlueprint> stateBoard() {
        return List.of(
            subject("Mathematics", "State board maths", "calculate", "#1565C0", 1, List.of(
                chapter("Algebra", "Equations", 1, List.of(
                    topic("Polynomials", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Linear Equations", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Quadratic Equations", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Geometry", "Shapes", 2, List.of(
                    topic("Triangles", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Circles", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Trigonometry Intro", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            )),
            subject("Science", "State board science", "science", "#43A047", 2, List.of(
                chapter("Physics", "Physical science", 1, List.of(
                    topic("Light & Sound", 2, Topic.DifficultyLevel.EASY, 1),
                    topic("Electricity", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Magnetism", 2, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Chemistry", "Matter", 2, List.of(
                    topic("Acids, Bases & Salts", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Metals & Non-metals", 2.5, Topic.DifficultyLevel.MEDIUM, 2)
                )),
                chapter("Biology", "Life science", 3, List.of(
                    topic("Life Processes", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Heredity & Evolution", 2.5, Topic.DifficultyLevel.MEDIUM, 2)
                ))
            )),
            subject("Social Science", "SST", "public", "#8D6E63", 3, List.of(
                chapter("History", "Past events", 1, List.of(
                    topic("Nationalism in India", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Print Culture", 2, Topic.DifficultyLevel.EASY, 2)
                )),
                chapter("Geography", "Earth & India", 2, List.of(
                    topic("Resources & Development", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Agriculture", 2, Topic.DifficultyLevel.EASY, 2)
                ))
            )),
            subject("English", "State board English", "menu_book", "#2E7D32", 4, List.of(
                chapter("Language Skills", "Communication", 1, List.of(
                    topic("Reading Skills", 2, Topic.DifficultyLevel.EASY, 1),
                    topic("Writing Skills", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Grammar", 2, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            ))
        );
    }

    private static List<SubjectBlueprint> rbse() {
        return List.of(
            subject("Mathematics", "RBSE maths", "calculate", "#1565C0", 1, List.of(
                chapter("Algebra & Trigonometry", "Core topics", 1, List.of(
                    topic("Relations & Functions", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Trigonometric Functions", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Matrices", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Calculus Intro", "Limits basics", 2, List.of(
                    topic("Continuity & Differentiability", 3, Topic.DifficultyLevel.HARD, 1),
                    topic("Applications of Derivatives", 3, Topic.DifficultyLevel.HARD, 2)
                ))
            )),
            subject("Science", "RBSE science", "science", "#43A047", 2, List.of(
                chapter("Physics", "RBSE physics", 1, List.of(
                    topic("Electrostatics", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Current Electricity", 2.5, Topic.DifficultyLevel.MEDIUM, 2)
                )),
                chapter("Chemistry", "RBSE chemistry", 2, List.of(
                    topic("Solutions", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Chemical Kinetics", 2.5, Topic.DifficultyLevel.MEDIUM, 2)
                )),
                chapter("Biology", "RBSE biology", 3, List.of(
                    topic("Reproduction", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Genetics", 3, Topic.DifficultyLevel.MEDIUM, 2)
                ))
            )),
            subject("Hindi", "RBSE Hindi", "translate", "#C62828", 3, List.of(
                chapter("Vyakaran", "Grammar", 1, List.of(
                    topic("Varna & Sandhi", 2, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Alankar", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Gadyansh & Patra", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            ))
        );
    }

    private static List<SubjectBlueprint> cuet() {
        return List.of(
            subject("English Language", "CUET English", "menu_book", "#2E7D32", 1, List.of(
                chapter("Language Test", "Proficiency", 1, List.of(
                    topic("Reading Comprehension", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Vocabulary", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Grammar & Usage", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            )),
            subject("General Test", "CUET GT", "quiz", "#6A1B9A", 2, List.of(
                chapter("Aptitude", "Mixed skills", 1, List.of(
                    topic("Quantitative Aptitude", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Logical Reasoning", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("General Awareness", 2.5, Topic.DifficultyLevel.EASY, 3)
                ))
            )),
            subject("Domain Subject (Science)", "CUET science", "science", "#5C6BC0", 3, List.of(
                chapter("Core Science", "PCM/B basics", 1, List.of(
                    topic("Physics Fundamentals", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Chemistry Fundamentals", 3, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Biology Fundamentals", 3, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            ))
        );
    }

    private static List<SubjectBlueprint> nda() {
        return List.of(
            subject("Mathematics", "NDA maths", "calculate", "#1565C0", 1, List.of(
                chapter("Algebra", "Core maths", 1, List.of(
                    topic("Sets & Relations", 2, Topic.DifficultyLevel.EASY, 1),
                    topic("Complex Numbers", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Quadratic Equations", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Trigonometry & Geometry", "Spatial maths", 2, List.of(
                    topic("Trigonometry", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Coordinate Geometry", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Mensuration", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("Calculus & Stats", "Advanced basics", 3, List.of(
                    topic("Differential Calculus", 3, Topic.DifficultyLevel.HARD, 1),
                    topic("Statistics", 2, Topic.DifficultyLevel.MEDIUM, 2)
                ))
            )),
            subject("General Ability Test", "NDA GAT", "military_tech", "#455A64", 2, List.of(
                chapter("English", "Language", 1, List.of(
                    topic("Grammar", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Vocabulary", 2, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Comprehension", 2.5, Topic.DifficultyLevel.MEDIUM, 3)
                )),
                chapter("General Knowledge", "GK", 2, List.of(
                    topic("History & Geography", 3, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Polity & Economy", 2.5, Topic.DifficultyLevel.MEDIUM, 2),
                    topic("Science & Current Affairs", 3, Topic.DifficultyLevel.MEDIUM, 3)
                ))
            )),
            subject("Science (Class 12 level)", "NDA science", "science", "#43A047", 3, List.of(
                chapter("Physics", "Applied physics", 1, List.of(
                    topic("Motion & Forces", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Energy & Waves", 2.5, Topic.DifficultyLevel.MEDIUM, 2)
                )),
                chapter("Chemistry", "Basics", 2, List.of(
                    topic("Physical Chemistry", 2.5, Topic.DifficultyLevel.MEDIUM, 1),
                    topic("Inorganic Chemistry", 2.5, Topic.DifficultyLevel.MEDIUM, 2)
                ))
            ))
        );
    }

    // ── Blueprint records & helpers ────────────────────────────────────────────

    private static SubjectBlueprint subject(String name, String desc, String icon, String color,
                                            int order, List<ChapterBlueprint> chapters) {
        return new SubjectBlueprint(name, desc, icon, color, order, chapters);
    }

    private static ChapterBlueprint chapter(String title, String desc, int order, List<TopicBlueprint> topics) {
        return new ChapterBlueprint(title, desc, order, topics);
    }

    private static TopicBlueprint topic(String title, double hours, Topic.DifficultyLevel diff, int order) {
        return new TopicBlueprint(title, hours, diff, order);
    }

    private record SubjectBlueprint(
        String name, String description, String icon, String color, int order, List<ChapterBlueprint> chapters
    ) {}

    private record ChapterBlueprint(String title, String description, int order, List<TopicBlueprint> topics) {}

    private record TopicBlueprint(String title, double hours, Topic.DifficultyLevel difficulty, int order) {}
}
