package com.examsaathi.config;

import com.examsaathi.entity.*;
import com.examsaathi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * DataInitializer — seeds roles, admin user, and CBSE syllabus on first boot.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;
    private final TopicRepository topicRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
        seedExams();
    }

    // ── Roles ────────────────────────────────────────────────────────────────
    private void seedRoles() {
        for (Role.RoleName roleName : Role.RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
                log.info("Seeded role: {}", roleName);
            }
        }
    }

    // ── Admin user ───────────────────────────────────────────────────────────
    private void seedAdminUser() {
        String adminEmail = "admin@examsaathi.com";
        if (userRepository.existsByEmail(adminEmail)) return;
        Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN).orElseThrow();
        userRepository.save(User.builder()
            .fullName("ExamSaathi Admin")
            .email(adminEmail)
            .password(passwordEncoder.encode("Admin@123"))
            .isActive(true)
            .isEmailVerified(true)
            .roles(Set.of(adminRole))
            .build());
        log.info("Admin user seeded: {} / Admin@123", adminEmail);
    }

    // ── Exams ────────────────────────────────────────────────────────────────
    private void seedExams() {
        if (examRepository.count() > 0) return;

        Exam cbse10 = examRepository.save(Exam.builder()
            .name("10th (CBSE)").code("CBSE10").colorCode("#6C63FF").isActive(true).build());
        Exam cbse12 = examRepository.save(Exam.builder()
            .name("12th (CBSE)").code("CBSE12").colorCode("#FF6584").isActive(true).build());

        seedCbse10(cbse10);
        seedCbse12(cbse12);
        log.info("Seeded CBSE 10th and 12th with full syllabus");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Subject sub(Exam exam, String name, String desc, String icon, String color, int order) {
        return subjectRepository.save(Subject.builder()
            .name(name).description(desc).iconName(icon).colorCode(color)
            .displayOrder(order).isActive(true).exam(exam).build());
    }

    private Chapter ch(Subject subject, String title, String desc, int order) {
        return chapterRepository.save(Chapter.builder()
            .title(title).description(desc).orderIndex(order).isActive(true).subject(subject).build());
    }

    private void t(Chapter chapter, String title, double hrs, Topic.DifficultyLevel diff, int order) {
        topicRepository.save(Topic.builder()
            .title(title).estimatedHours(hrs)
            .difficultyLevel(diff).orderIndex(order).isActive(true).chapter(chapter).build());
    }

    // ── CBSE Class 10 ────────────────────────────────────────────────────────
    private void seedCbse10(Exam exam) {
        // ── Mathematics ──────────────────────────────────────────────────────
        Subject math = sub(exam, "Mathematics", "CBSE Class 10 Mathematics – Number systems, Algebra, Geometry, Trigonometry, Statistics", "calculate", "#1565C0", 1);
        Chapter c;
        c = ch(math, "Real Numbers", "Euclid's division lemma, fundamental theorem of arithmetic", 1);
            t(c, "Euclid's Division Lemma", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Fundamental Theorem of Arithmetic", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Irrational Numbers", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Decimal Expansions of Rational Numbers", 1.0, Topic.DifficultyLevel.EASY, 4);
        c = ch(math, "Polynomials", "Zeroes of polynomials, relationship between zeroes and coefficients", 2);
            t(c, "Zeroes of a Polynomial", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Relationship Between Zeroes and Coefficients", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Division Algorithm for Polynomials", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(math, "Pair of Linear Equations in Two Variables", "Graphical method, algebraic methods – substitution, elimination, cross-multiplication", 3);
            t(c, "Graphical Method of Solution", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Substitution Method", 1.0, Topic.DifficultyLevel.EASY, 2);
            t(c, "Elimination Method", 1.0, Topic.DifficultyLevel.EASY, 3);
            t(c, "Cross-Multiplication Method", 1.5, Topic.DifficultyLevel.HARD, 4);
            t(c, "Equations Reducible to Linear Form", 1.5, Topic.DifficultyLevel.HARD, 5);
        c = ch(math, "Quadratic Equations", "Standard form, factorisation, quadratic formula, discriminant", 4);
            t(c, "Standard Form of Quadratic Equation", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Solution by Factorisation", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Solution by Completing the Square", 1.5, Topic.DifficultyLevel.HARD, 3);
            t(c, "Quadratic Formula and Discriminant", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
            t(c, "Word Problems on Quadratic Equations", 2.0, Topic.DifficultyLevel.HARD, 5);
        c = ch(math, "Arithmetic Progressions", "nth term, sum of n terms", 5);
            t(c, "Introduction to AP", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "nth Term of an AP", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Sum of First n Terms", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Word Problems on AP", 2.0, Topic.DifficultyLevel.HARD, 4);
        c = ch(math, "Triangles", "Similarity, Thales theorem, Pythagoras theorem", 6);
            t(c, "Similar Figures", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Basic Proportionality (Thales) Theorem", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Criteria for Similarity of Triangles", 2.0, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Areas of Similar Triangles", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
            t(c, "Pythagoras Theorem", 1.5, Topic.DifficultyLevel.MEDIUM, 5);
        c = ch(math, "Coordinate Geometry", "Distance formula, section formula, area of triangle", 7);
            t(c, "Distance Formula", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Section Formula", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Area of a Triangle", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(math, "Introduction to Trigonometry", "Trigonometric ratios, identities", 8);
            t(c, "Trigonometric Ratios", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Trigonometric Ratios of Specific Angles", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Trigonometric Ratios of Complementary Angles", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Trigonometric Identities", 2.0, Topic.DifficultyLevel.HARD, 4);
        c = ch(math, "Some Applications of Trigonometry", "Heights and distances", 9);
            t(c, "Angle of Elevation and Depression", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Heights and Distances Problems", 2.5, Topic.DifficultyLevel.HARD, 2);
        c = ch(math, "Circles", "Tangent to a circle, theorems", 10);
            t(c, "Tangent to a Circle", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Number of Tangents from External Point", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Theorems on Tangents", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(math, "Areas Related to Circles", "Perimeter and area of circle, sector, segment", 11);
            t(c, "Perimeter and Area of a Circle", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Areas of Sectors and Segments", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Areas of Combinations of Plane Figures", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(math, "Surface Areas and Volumes", "Combinations of solids, conversion of solids", 12);
            t(c, "Surface Area of Combination of Solids", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Volume of Combination of Solids", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Conversion of Solid from One Shape to Another", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Frustum of a Cone", 2.0, Topic.DifficultyLevel.HARD, 4);
        c = ch(math, "Statistics", "Mean, median, mode of grouped data", 13);
            t(c, "Mean of Grouped Data", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Mode of Grouped Data", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Median of Grouped Data", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Ogives (Cumulative Frequency Curves)", 1.5, Topic.DifficultyLevel.HARD, 4);
        c = ch(math, "Probability", "Classical definition, simple problems", 14);
            t(c, "Probability – An Introduction", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Theoretical Probability", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Problems on Playing Cards and Dice", 2.0, Topic.DifficultyLevel.HARD, 3);

        // ── Science ──────────────────────────────────────────────────────────
        Subject sci = sub(exam, "Science", "CBSE Class 10 Science – Physics, Chemistry and Biology", "science", "#2E7D32", 2);
        // Chemistry
        c = ch(sci, "Chemical Reactions and Equations", "Types of chemical reactions, balancing equations", 1);
            t(c, "Chemical Equations and Balancing", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Types of Chemical Reactions", 2.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Oxidation and Reduction", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(sci, "Acids, Bases and Salts", "Properties, reactions, pH scale", 2);
            t(c, "Properties of Acids and Bases", 1.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Reaction of Acids and Bases", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "pH Scale and Importance", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Salts and Their Preparation", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
        c = ch(sci, "Metals and Non-metals", "Physical and chemical properties, reactivity series", 3);
            t(c, "Physical Properties of Metals and Non-metals", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Chemical Properties of Metals", 2.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Reactivity Series", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Ionic Compounds and Occurrence of Metals", 1.5, Topic.DifficultyLevel.HARD, 4);
        c = ch(sci, "Carbon and Its Compounds", "Bonding, functional groups, IUPAC nomenclature, important compounds", 4);
            t(c, "Bonding in Carbon – Covalent Bonds", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Functional Groups and Homologous Series", 2.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Nomenclature of Carbon Compounds", 1.5, Topic.DifficultyLevel.HARD, 3);
            t(c, "Chemical Properties of Carbon Compounds", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
            t(c, "Important Carbon Compounds – Ethanol, Ethanoic Acid", 1.0, Topic.DifficultyLevel.EASY, 5);
        c = ch(sci, "Periodic Classification of Elements", "Dobereiner's triads, Mendeleev's table, Modern periodic table", 5);
            t(c, "Early Attempts at Classification", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Mendeleev's Periodic Table", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Modern Periodic Table", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Trends in the Modern Periodic Table", 2.0, Topic.DifficultyLevel.HARD, 4);
        // Biology
        c = ch(sci, "Life Processes", "Nutrition, respiration, transportation, excretion", 6);
            t(c, "Nutrition in Plants and Animals", 2.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Respiration – Aerobic and Anaerobic", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Transportation in Plants and Animals", 2.0, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Excretion in Plants and Animals", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
        c = ch(sci, "Control and Coordination", "Nervous system, hormones in animals and plants", 7);
            t(c, "Nervous System – Neurons and Brain", 2.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Reflex Action and Reflex Arc", 1.0, Topic.DifficultyLevel.EASY, 2);
            t(c, "Hormones in Animals", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Coordination in Plants", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
        c = ch(sci, "How do Organisms Reproduce?", "Asexual and sexual reproduction in plants and animals", 8);
            t(c, "Asexual Reproduction", 1.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Sexual Reproduction in Flowering Plants", 2.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Reproduction in Human Beings", 2.0, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Reproductive Health", 1.0, Topic.DifficultyLevel.EASY, 4);
        c = ch(sci, "Heredity and Evolution", "Mendel's laws, sex determination, evolution", 9);
            t(c, "Mendel's Experiments and Laws", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "Sex Determination", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Evolution – Theories and Evidence", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Speciation and Human Evolution", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
        // Physics
        c = ch(sci, "Light – Reflection and Refraction", "Laws of reflection, mirrors, refraction, lenses", 10);
            t(c, "Reflection of Light and Mirror Formula", 2.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Refraction of Light and Snell's Law", 2.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Refraction Through Lenses and Lens Formula", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Power of a Lens", 1.0, Topic.DifficultyLevel.MEDIUM, 4);
        c = ch(sci, "Human Eye and the Colourful World", "Defects of vision, atmospheric refraction, dispersion", 11);
            t(c, "Human Eye and its Working", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Defects of Vision and Their Correction", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Refraction Through Prism and Scattering of Light", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(sci, "Electricity", "Ohm's law, circuits, power, heating effect", 12);
            t(c, "Electric Current and Circuit", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Ohm's Law and Resistance", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Series and Parallel Combination of Resistors", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Heating Effect of Electric Current", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
        c = ch(sci, "Magnetic Effects of Electric Current", "Magnetic field, electromagnets, motors, generators", 13);
            t(c, "Magnetic Field and Field Lines", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Magnetic Effect of Current – Oersted's Experiment", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Force on Current-Carrying Conductor and Electric Motor", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Electromagnetic Induction and Electric Generator", 1.5, Topic.DifficultyLevel.HARD, 4);
        c = ch(sci, "Our Environment", "Food chains, ozone layer, waste management", 14);
            t(c, "Ecosystems – Components and Food Chain", 1.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Ozone Layer and its Depletion", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Waste Management and Environmental Impact", 1.5, Topic.DifficultyLevel.MEDIUM, 3);

        // ── Social Science ────────────────────────────────────────────────────
        Subject sst = sub(exam, "Social Science", "CBSE Class 10 – History, Geography, Political Science and Economics", "public", "#E64A19", 3);
        // History
        c = ch(sst, "The Rise of Nationalism in Europe", "French Revolution, nation-states, cultural movements", 1);
            t(c, "French Revolution and Idea of Nation", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Nationalism and Imperialism in Europe", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Making of Germany and Italy", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(sst, "Nationalism in India", "Non-Cooperation, Civil Disobedience, INC movements", 2);
            t(c, "First World War, Khilafat and Non-Cooperation", 2.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Civil Disobedience Movement", 2.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Sense of Collective Belonging", 1.0, Topic.DifficultyLevel.EASY, 3);
        c = ch(sst, "The Making of a Global World", "Trade and global economy, interwar economy", 3);
            t(c, "Pre-modern World – Silk Routes and Trade", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Nineteenth Century Globalisation", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Interwar Economy and Great Depression", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(sst, "The Age of Industrialisation", "Proto-industrialisation, factory systems, Indian textiles", 4);
            t(c, "Proto-Industrialisation in England", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Hand Labour and Steam Power", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Industrialisation in India – Textile Industry", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(sst, "Print Culture and the Modern World", "Printing press, newspapers, India", 5);
            t(c, "First Printed Books and Print Revolution", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Print and Dissent in Europe", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Print Culture and Nationalism in India", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        // Geography
        c = ch(sst, "Resources and Development", "Types of resources, resource planning, soil conservation", 6);
            t(c, "Types and Classification of Resources", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Resource Planning in India", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Land Resources and Soil Types", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(sst, "Water Resources", "Dams, rainwater harvesting, water scarcity", 7);
            t(c, "Water Scarcity and Conservation", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Multi-Purpose River Projects and Dams", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Rainwater Harvesting", 1.0, Topic.DifficultyLevel.EASY, 3);
        c = ch(sst, "Agriculture", "Types of farming, crops, food security", 8);
            t(c, "Types of Farming in India", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Cropping Seasons and Major Crops", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Technological and Institutional Reforms", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(sst, "Minerals and Energy Resources", "Types of minerals, conventional and non-conventional energy", 9);
            t(c, "Types and Distribution of Minerals", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Conventional Sources of Energy", 1.0, Topic.DifficultyLevel.EASY, 2);
            t(c, "Non-Conventional Sources of Energy", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(sst, "Manufacturing Industries", "Types of industries, industrial pollution", 10);
            t(c, "Importance and Classification of Industries", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Textile, Iron-Steel and Automobile Industries", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Industrial Pollution and Control", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(sst, "Lifelines of National Economy", "Transport, communication, trade", 11);
            t(c, "Roadways, Railways and Airways", 1.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Pipelines, Waterways and Communication", 1.0, Topic.DifficultyLevel.EASY, 2);
            t(c, "International and Domestic Trade", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        // Political Science
        c = ch(sst, "Power Sharing", "Horizontal and vertical power sharing, Belgium and Sri Lanka", 12);
            t(c, "Belgium and Sri Lanka Case Studies", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Forms of Power Sharing", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
        c = ch(sst, "Federalism", "Types of federation, decentralisation in India", 13);
            t(c, "What is Federalism?", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Federal Features of India", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Decentralisation in India", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(sst, "Democracy and Diversity", "Social divisions, overlapping and cross-cutting differences", 14);
            t(c, "Social Divisions and Democracy", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Outcomes of Social Divisions in Politics", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
        c = ch(sst, "Gender, Religion and Caste", "Gender, religion and caste in Indian politics", 15);
            t(c, "Gender and Politics", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Religion, Communalism and Politics", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Caste and Politics", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(sst, "Political Parties", "Types of parties, national and state parties, challenges", 16);
            t(c, "Why Do We Need Political Parties?", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "National and State Parties in India", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Challenges to and Reforms for Political Parties", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(sst, "Outcomes of Democracy", "Accountable, responsive and legitimate government", 17);
            t(c, "How Do We Assess Democracy's Outcomes?", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Quality of Government, Economy and Dignity", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
        // Economics
        c = ch(sst, "Development", "Concept of development, national income, HDI", 18);
            t(c, "What is Development?", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Income and Other Goals", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "National Development and Sustainability", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(sst, "Sectors of the Indian Economy", "Primary, secondary, tertiary sectors, employment", 19);
            t(c, "Sectors of Economic Activities", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Comparing Sectors – GDP and Employment", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Organised and Unorganised Sectors", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(sst, "Money and Credit", "Barter system, money, banking, credit", 20);
            t(c, "Money as a Medium of Exchange", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Formal and Informal Sources of Credit", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Self-Help Groups and Microfinance", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(sst, "Globalisation and the Indian Economy", "MNCs, trade, liberalisation, impact", 21);
            t(c, "Production Across Countries and MNCs", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Foreign Trade and Integration of Markets", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Impact of Globalisation and WTO", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(sst, "Consumer Rights", "Consumer protection, COPRA, consumer awareness", 22);
            t(c, "Consumer Movement and Rights", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Consumer Protection Act (COPRA)", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Consumer Awareness and Redressal Forum", 1.0, Topic.DifficultyLevel.MEDIUM, 3);

        // ── English Language & Literature ─────────────────────────────────────
        Subject eng = sub(exam, "English Language & Literature", "CBSE Class 10 English – First Flight and Footprints Without Feet", "menu_book", "#6A1B9A", 4);
        c = ch(eng, "First Flight – Prose", "Chapters from the First Flight reader", 1);
            t(c, "A Letter to God", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Nelson Mandela – Long Walk to Freedom", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Two Stories about Flying", 1.0, Topic.DifficultyLevel.EASY, 3);
            t(c, "From the Diary of Anne Frank", 1.0, Topic.DifficultyLevel.MEDIUM, 4);
            t(c, "Glimpses of India", 1.0, Topic.DifficultyLevel.EASY, 5);
            t(c, "Mijbil the Otter", 1.0, Topic.DifficultyLevel.EASY, 6);
            t(c, "Madam Rides the Bus", 1.0, Topic.DifficultyLevel.MEDIUM, 7);
            t(c, "The Sermon at Benares", 1.0, Topic.DifficultyLevel.MEDIUM, 8);
            t(c, "The Proposal", 1.0, Topic.DifficultyLevel.MEDIUM, 9);
        c = ch(eng, "First Flight – Poetry", "Poems from First Flight", 2);
            t(c, "Dust of Snow", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Fire and Ice", 0.5, Topic.DifficultyLevel.EASY, 2);
            t(c, "A Tiger in the Zoo", 0.5, Topic.DifficultyLevel.EASY, 3);
            t(c, "How to Tell Wild Animals", 0.5, Topic.DifficultyLevel.EASY, 4);
            t(c, "The Ball Poem", 0.5, Topic.DifficultyLevel.MEDIUM, 5);
            t(c, "Amanda!", 0.5, Topic.DifficultyLevel.MEDIUM, 6);
            t(c, "Animals", 0.5, Topic.DifficultyLevel.EASY, 7);
            t(c, "The Trees", 0.5, Topic.DifficultyLevel.MEDIUM, 8);
            t(c, "Fog", 0.5, Topic.DifficultyLevel.EASY, 9);
            t(c, "The Tale of Custard the Dragon", 0.5, Topic.DifficultyLevel.EASY, 10);
            t(c, "For Anne Gregory", 0.5, Topic.DifficultyLevel.MEDIUM, 11);
        c = ch(eng, "Footprints Without Feet – Supplementary Reader", "Supplementary stories", 3);
            t(c, "A Triumph of Surgery", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "The Thief's Story", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "The Midnight Visitor", 1.0, Topic.DifficultyLevel.EASY, 3);
            t(c, "A Question of Trust", 1.0, Topic.DifficultyLevel.MEDIUM, 4);
            t(c, "Footprints Without Feet", 1.0, Topic.DifficultyLevel.MEDIUM, 5);
            t(c, "The Making of a Scientist", 1.0, Topic.DifficultyLevel.MEDIUM, 6);
            t(c, "The Necklace", 1.0, Topic.DifficultyLevel.HARD, 7);
            t(c, "The Hack Driver", 1.0, Topic.DifficultyLevel.MEDIUM, 8);
            t(c, "Bholi", 1.0, Topic.DifficultyLevel.EASY, 9);
            t(c, "The Book That Saved the Earth", 1.0, Topic.DifficultyLevel.EASY, 10);
        c = ch(eng, "Grammar and Writing Skills", "Tenses, modals, letter writing, article, notice", 4);
            t(c, "Tenses – All Forms", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Modals and Determiners", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Subject-Verb Concord and Reported Speech", 1.5, Topic.DifficultyLevel.HARD, 3);
            t(c, "Letter Writing – Formal and Informal", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
            t(c, "Article, Notice and Paragraph Writing", 1.5, Topic.DifficultyLevel.MEDIUM, 5);

        // ── Hindi Course A ─────────────────────────────────────────────────────
        Subject hindi = sub(exam, "Hindi Course A", "CBSE Class 10 Hindi – Kshitij and Kritika", "translate", "#AD1457", 5);
        c = ch(hindi, "Kshitij – Gadya (Prose)", "Prose chapters from Kshitij Part 2", 1);
            t(c, "Surdas – Pado", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Tulsidas – Ram Lakshman Parashuram Samvad", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Dev – Savaiya aur Kavitt", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Jaishankar Prasad – Aatmakadhy", 1.0, Topic.DifficultyLevel.HARD, 4);
            t(c, "Sumitranandan Pant – Utsah and At Nahi Rahi Hai", 1.0, Topic.DifficultyLevel.MEDIUM, 5);
        c = ch(hindi, "Kshitij – Kavya (Poetry)", "Poetry chapters from Kshitij Part 2", 2);
            t(c, "Suryakant Tripathi Nirala – Utasah", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Nagarjun – Yeh Daant ki Jad", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Girija Kumar Mathur – Chaya Mat Chuna", 1.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(hindi, "Kritika – Supplementary Reader", "Chapters from Kritika Part 2", 3);
            t(c, "Mata Ka Anchal", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "George Pancham Ki Naak", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Sana Sana Hath Jodi", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Ehi Thaiyan Jhulni Herani Ho Rama", 1.0, Topic.DifficultyLevel.MEDIUM, 4);
            t(c, "Main Kyon Likhta Hoon", 1.0, Topic.DifficultyLevel.EASY, 5);
        c = ch(hindi, "Vyakaran (Grammar)", "Rachna, samas, sandhi, alankar, ras", 4);
            t(c, "Sandhi", 1.5, Topic.DifficultyLevel.HARD, 1);
            t(c, "Samas (Compound Words)", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Alankar (Figures of Speech)", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Ras (Sentiment)", 1.5, Topic.DifficultyLevel.HARD, 4);
    }

    // ── CBSE Class 12 ────────────────────────────────────────────────────────
    private void seedCbse12(Exam exam) {
        // ── Physics ──────────────────────────────────────────────────────────
        Subject phy = sub(exam, "Physics", "CBSE Class 12 Physics – Electrostatics, Optics, Electronics and Modern Physics", "bolt", "#1565C0", 1);
        Chapter c;
        c = ch(phy, "Electric Charges and Fields", "Coulomb's law, electric field and flux, Gauss's theorem", 1);
            t(c, "Electric Charge and Coulomb's Law", 2.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Electric Field Lines and Electric Flux", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Gauss's Theorem and Applications", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(phy, "Electrostatic Potential and Capacitance", "Potential, potential energy, capacitors", 2);
            t(c, "Electrostatic Potential and Potential Difference", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Relation Between E and V, Equipotential Surfaces", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Capacitors – Series and Parallel", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Energy Stored in a Capacitor", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
        c = ch(phy, "Current Electricity", "Ohm's law, Kirchhoff's laws, Wheatstone bridge", 3);
            t(c, "Electric Current and Drift Velocity", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Resistivity, Conductivity and Colour Codes", 1.0, Topic.DifficultyLevel.EASY, 2);
            t(c, "Kirchhoff's Laws and Wheatstone Bridge", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Potentiometer", 1.5, Topic.DifficultyLevel.HARD, 4);
        c = ch(phy, "Moving Charges and Magnetism", "Biot-Savart law, Ampere's law, cyclotron, galvanometer", 4);
            t(c, "Biot-Savart Law and Magnetic Field of a Solenoid", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "Ampere's Circuital Law", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Force on a Charged Particle – Lorentz Force", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Galvanometer, Ammeter and Voltmeter", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
        c = ch(phy, "Magnetism and Matter", "Bar magnet, earth's magnetism, para/dia/ferromagnetism", 5);
            t(c, "Bar Magnet and Magnetic Field Lines", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Earth's Magnetism and Magnetic Elements", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Magnetic Properties of Materials", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(phy, "Electromagnetic Induction", "Faraday's laws, Lenz's law, eddy currents, inductance", 6);
            t(c, "Magnetic Flux and Faraday's Law of Induction", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Lenz's Law and Eddy Currents", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Inductance – Self and Mutual", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(phy, "Alternating Current", "AC circuit, power, transformers, resonance", 7);
            t(c, "AC Voltage and Phasor Diagrams", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "AC Circuits – R, L, C and Series LCR", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Power in AC Circuit and Resonance", 1.5, Topic.DifficultyLevel.HARD, 3);
            t(c, "Transformers", 1.0, Topic.DifficultyLevel.MEDIUM, 4);
        c = ch(phy, "Electromagnetic Waves", "Displacement current, spectrum of EM waves", 8);
            t(c, "Displacement Current and Maxwell's Equations", 1.5, Topic.DifficultyLevel.HARD, 1);
            t(c, "Electromagnetic Spectrum", 1.0, Topic.DifficultyLevel.EASY, 2);
        c = ch(phy, "Ray Optics and Optical Instruments", "Reflection, refraction, TIR, lens maker's formula, instruments", 9);
            t(c, "Reflection and Refraction at Spherical Surfaces", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "Total Internal Reflection and Optical Fibres", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Lens Maker's Formula and Power of Lenses", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Microscopes and Telescopes", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
        c = ch(phy, "Wave Optics", "Wavefront, Huygens' principle, interference, diffraction, polarisation", 10);
            t(c, "Huygens' Principle and Wavefront", 1.5, Topic.DifficultyLevel.HARD, 1);
            t(c, "Young's Double Slit Experiment", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Diffraction and Single Slit Experiment", 1.5, Topic.DifficultyLevel.HARD, 3);
            t(c, "Polarisation of Light", 1.0, Topic.DifficultyLevel.MEDIUM, 4);
        c = ch(phy, "Dual Nature of Radiation and Matter", "Photoelectric effect, de Broglie wavelength", 11);
            t(c, "Photoelectric Effect and Einstein's Equation", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "Wave Theory of Matter – de Broglie Hypothesis", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Davisson-Germer Experiment", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(phy, "Atoms", "Rutherford model, Bohr model, hydrogen spectrum", 12);
            t(c, "Alpha Particle Scattering and Rutherford's Model", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Bohr's Model of Hydrogen Atom", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Hydrogen Spectrum and Spectral Series", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(phy, "Nuclei", "Nuclear composition, binding energy, radioactivity, nuclear reactions", 13);
            t(c, "Nuclear Size, Mass and Binding Energy", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Radioactivity – Alpha, Beta, Gamma Decay", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Nuclear Fission and Fusion", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(phy, "Semiconductor Electronics", "Semiconductor devices, diodes, transistors, logic gates", 14);
            t(c, "Semiconductors – Intrinsic and Extrinsic", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "p-n Junction Diode and Rectifier", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Transistor – Characteristics and Amplifier", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Logic Gates", 1.0, Topic.DifficultyLevel.MEDIUM, 4);

        // ── Chemistry ────────────────────────────────────────────────────────
        Subject chem = sub(exam, "Chemistry", "CBSE Class 12 Chemistry – Physical, Organic and Inorganic Chemistry", "science", "#2E7D32", 2);
        c = ch(chem, "The Solid State", "Types of solids, crystal systems, defects", 1);
            t(c, "Classification of Solids", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Crystal Lattices and Unit Cell", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Packing Efficiency and Imperfections", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(chem, "Solutions", "Types, Raoult's law, colligative properties", 2);
            t(c, "Types of Solutions and Concentration Terms", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Raoult's Law and Ideal Solutions", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Colligative Properties – Osmosis, Ebullioscopy", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(chem, "Electrochemistry", "Galvanic cells, EMF, electrolytic conduction, corrosion", 3);
            t(c, "Electrochemical Cells and EMF", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Nernst Equation and Electrode Potential", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Electrolytic Conduction and Kohlrausch's Law", 1.5, Topic.DifficultyLevel.HARD, 3);
            t(c, "Electrolysis and Corrosion", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
        c = ch(chem, "Chemical Kinetics", "Rate laws, order, activation energy, Arrhenius equation", 4);
            t(c, "Rate of Reaction and Rate Law", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Integrated Rate Equations", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Collision Theory and Activation Energy", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(chem, "Surface Chemistry", "Adsorption, catalysis, colloids", 5);
            t(c, "Adsorption – Physisorption and Chemisorption", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Catalysis – Homogeneous and Heterogeneous", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Colloids and Emulsions", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(chem, "General Principles of Isolation of Elements", "Metallurgy – roasting, smelting, refining", 6);
            t(c, "Occurrence of Metals and Minerals", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Thermodynamic and Electrochemical Principles", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Refining of Metals", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(chem, "The p-Block Elements", "Groups 15–18 elements: properties and compounds", 7);
            t(c, "Group 15 – Nitrogen Family", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "Group 16 – Oxygen Family", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Group 17 – Halogens", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Group 18 – Noble Gases", 1.0, Topic.DifficultyLevel.MEDIUM, 4);
        c = ch(chem, "The d- and f-Block Elements", "Transition metals, lanthanoids, actinoids", 8);
            t(c, "Transition Metals – General Properties", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Important Compounds of Transition Metals", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Lanthanoids and Actinoids", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(chem, "Coordination Compounds", "Werner's theory, IUPAC nomenclature, bonding theories, isomerism", 9);
            t(c, "Werner's Theory and Terminology", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "IUPAC Nomenclature of Coordination Compounds", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Bonding Theories – VBT and CFT", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Isomerism in Coordination Compounds", 1.5, Topic.DifficultyLevel.HARD, 4);
        c = ch(chem, "Haloalkanes and Haloarenes", "Nomenclature, properties, reactions, SN1 and SN2", 10);
            t(c, "Classification and Nomenclature", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Preparation of Haloalkanes", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Chemical Reactions – SN1, SN2, Elimination", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(chem, "Alcohols, Phenols and Ethers", "Preparation, physical and chemical properties", 11);
            t(c, "Alcohols – Preparation and Properties", 2.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Phenols – Preparation and Reactions", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Ethers – Preparation and Reactions", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(chem, "Aldehydes, Ketones and Carboxylic Acids", "Nucleophilic addition, aldol condensation, acidity of carboxylic acids", 12);
            t(c, "Aldehydes and Ketones – Preparation and Properties", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "Nucleophilic Addition and Aldol Reaction", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Carboxylic Acids – Properties and Reactions", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(chem, "Amines", "Classification, preparation, properties, diazonium salts", 13);
            t(c, "Classification and Nomenclature of Amines", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Preparation and Chemical Properties of Amines", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Diazonium Salts and Their Reactions", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(chem, "Biomolecules", "Carbohydrates, proteins, lipids, nucleic acids", 14);
            t(c, "Carbohydrates – Classification and Structure", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Proteins – Amino Acids and Structure", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Enzymes, Vitamins and Nucleic Acids", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(chem, "Polymers", "Classification, types of polymerisation, commercial polymers", 15);
            t(c, "Classification of Polymers", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Addition and Condensation Polymerisation", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Important Commercial Polymers", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(chem, "Chemistry in Everyday Life", "Drugs, food chemicals, cleansing agents", 16);
            t(c, "Drugs – Classification and Action", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Chemicals in Food – Preservatives and Antioxidants", 0.5, Topic.DifficultyLevel.EASY, 2);
            t(c, "Soaps and Detergents", 1.0, Topic.DifficultyLevel.EASY, 3);

        // ── Mathematics ──────────────────────────────────────────────────────
        Subject math = sub(exam, "Mathematics", "CBSE Class 12 Mathematics – Calculus, Algebra, Vectors, Linear Programming", "calculate", "#1976D2", 3);
        c = ch(math, "Relations and Functions", "Types of relations, functions, binary operations", 1);
            t(c, "Types of Relations", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Types of Functions – Injective, Surjective, Bijective", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Composition of Functions and Invertible Functions", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(math, "Inverse Trigonometric Functions", "Principal value, properties, simplification", 2);
            t(c, "Definition and Principal Value", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Properties and Formulae", 2.0, Topic.DifficultyLevel.HARD, 2);
        c = ch(math, "Matrices", "Types, operations, elementary transformations, inverse", 3);
            t(c, "Types of Matrices and Operations", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Transpose and Symmetric Matrices", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Inverse of a Matrix by Elementary Operations", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(math, "Determinants", "Properties, cofactors, adjoint, Cramer's rule", 4);
            t(c, "Evaluation and Properties of Determinants", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "Adjoint and Inverse of a Matrix", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Solution of System of Equations using Determinants", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(math, "Continuity and Differentiability", "Continuity, differentiability, derivatives of special functions", 5);
            t(c, "Continuity of Functions", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Differentiability and Chain Rule", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Derivatives of Implicit, Parametric and Logarithmic Functions", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Mean Value Theorems – Rolle's and Lagrange's", 1.5, Topic.DifficultyLevel.HARD, 4);
        c = ch(math, "Application of Derivatives", "Rate of change, tangents, increasing/decreasing, maxima/minima", 6);
            t(c, "Rate of Change of Quantities", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Tangents and Normals", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Increasing and Decreasing Functions", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Maxima and Minima", 2.0, Topic.DifficultyLevel.HARD, 4);
        c = ch(math, "Integrals", "Indefinite and definite integrals, special methods", 7);
            t(c, "Integration as Reverse of Differentiation", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Methods of Integration – Substitution, Parts", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Integration by Partial Fractions", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Definite Integrals and Properties", 2.0, Topic.DifficultyLevel.HARD, 4);
        c = ch(math, "Application of Integrals", "Area under curves, area between curves", 8);
            t(c, "Area Under a Curve", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Area Between Two Curves", 2.0, Topic.DifficultyLevel.HARD, 2);
        c = ch(math, "Differential Equations", "Order, degree, formation, solutions", 9);
            t(c, "Basic Concepts – Order and Degree", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Solution of Differential Equations", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Variable Separable and Homogeneous Equations", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Linear Differential Equations", 2.0, Topic.DifficultyLevel.HARD, 4);
        c = ch(math, "Vector Algebra", "Vectors, dot product, cross product", 10);
            t(c, "Types of Vectors and Basic Operations", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Dot (Scalar) Product", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Cross (Vector) Product", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(math, "Three Dimensional Geometry", "Direction cosines, planes, lines, angles", 11);
            t(c, "Direction Cosines and Ratios", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Equation of a Line in Space", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Angle Between Lines, Planes and Skew Lines", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Equation of a Plane", 2.0, Topic.DifficultyLevel.HARD, 4);
        c = ch(math, "Linear Programming", "Graphical method, corner point, feasible region", 12);
            t(c, "Linear Programming Problem and its Formulation", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Graphical Method of Solving LPP", 2.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Different Types of LPP Problems", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(math, "Probability", "Conditional probability, Bayes' theorem, probability distribution", 13);
            t(c, "Conditional Probability and Multiplication Theorem", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Bayes' Theorem", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Random Variable and Probability Distribution", 1.5, Topic.DifficultyLevel.HARD, 3);
            t(c, "Binomial Distribution", 1.5, Topic.DifficultyLevel.HARD, 4);

        // ── Biology ──────────────────────────────────────────────────────────
        Subject bio = sub(exam, "Biology", "CBSE Class 12 Biology – Reproduction, Genetics, Evolution, Ecology and Biotechnology", "biotech", "#1B5E20", 4);
        c = ch(bio, "Reproduction in Organisms", "Modes of reproduction, asexual and sexual", 1);
            t(c, "Modes of Reproduction", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Asexual Reproduction", 1.0, Topic.DifficultyLevel.EASY, 2);
            t(c, "Sexual Reproduction – Phases and Events", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(bio, "Sexual Reproduction in Flowering Plants", "Structure, pollination, fertilisation, embryo development", 2);
            t(c, "Flower Structure – Stamen and Pistil", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Pollination – Types and Agents", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Double Fertilisation and Seed Development", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(bio, "Human Reproduction", "Male and female reproductive systems, gametogenesis, fertilisation", 3);
            t(c, "Male Reproductive System", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Female Reproductive System and Oogenesis", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Menstrual Cycle and Fertilisation", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Implantation and Embryonic Development", 1.5, Topic.DifficultyLevel.HARD, 4);
        c = ch(bio, "Reproductive Health", "STIs, contraception, MTP, ART", 4);
            t(c, "Reproductive Health and STIs", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Contraception Methods", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Infertility and Assisted Reproductive Technologies", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(bio, "Principles of Inheritance and Variation", "Mendel's laws, chromosomal theory, linkage", 5);
            t(c, "Mendel's Laws of Inheritance", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "Chromosomal Theory of Inheritance", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Linkage and Crossing Over", 1.5, Topic.DifficultyLevel.HARD, 3);
            t(c, "Mutations and Genetic Disorders", 1.5, Topic.DifficultyLevel.HARD, 4);
        c = ch(bio, "Molecular Basis of Inheritance", "DNA structure, replication, transcription, translation, genetic code", 6);
            t(c, "DNA Structure and Packaging", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "DNA Replication", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Transcription and RNA Processing", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Genetic Code and Translation", 2.0, Topic.DifficultyLevel.HARD, 4);
            t(c, "Regulation of Gene Expression", 1.5, Topic.DifficultyLevel.HARD, 5);
        c = ch(bio, "Evolution", "Origin of life, Darwinism, speciation, human evolution", 7);
            t(c, "Origin of Life – Chemical Evolution", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Darwin's Theory of Natural Selection", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Mechanisms of Evolution – Hardy-Weinberg", 2.0, Topic.DifficultyLevel.HARD, 3);
            t(c, "Human Evolution", 1.5, Topic.DifficultyLevel.MEDIUM, 4);
        c = ch(bio, "Human Health and Disease", "Immunity, AIDS, cancer, drugs, alcohol", 8);
            t(c, "Common Diseases and Immunity", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Immunology – Active and Passive Immunity", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "AIDS and Cancer", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Drug and Alcohol Abuse", 0.5, Topic.DifficultyLevel.EASY, 4);
        c = ch(bio, "Strategies for Enhancement in Food Production", "Plant breeding, biofortification, SCP, tissue culture", 9);
            t(c, "Animal Husbandry and Plant Breeding", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Tissue Culture and Biofortification", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Single Cell Protein (SCP)", 0.5, Topic.DifficultyLevel.EASY, 3);
        c = ch(bio, "Microbes in Human Welfare", "Biogas, biofertilisers, sewage treatment, antibiotics", 10);
            t(c, "Microbes in Household and Industrial Products", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Biogas and Sewage Treatment", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Biofertilisers and Biocontrol Agents", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(bio, "Biotechnology – Principles and Processes", "Recombinant DNA, PCR, gel electrophoresis", 11);
            t(c, "Recombinant DNA Technology – Tools and Enzymes", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "PCR and Gel Electrophoresis", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Cloning Vectors and Bioreactors", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(bio, "Biotechnology and its Applications", "GM crops, gene therapy, ELISA, ethical issues", 12);
            t(c, "Genetically Modified Organisms in Agriculture", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Gene Therapy and Molecular Diagnosis", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Transgenic Animals and Ethical Issues", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(bio, "Organisms and Populations", "Ecology, adaptations, population attributes, interactions", 13);
            t(c, "Organism and its Environment", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Population Attributes and Growth Models", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Population Interactions", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(bio, "Ecosystem", "Structure, energy flow, nutrient cycling, ecological succession", 14);
            t(c, "Ecosystem – Structure and Function", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Energy Flow and Productivity", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Nutrient Cycling and Ecosystem Services", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(bio, "Biodiversity and Conservation", "Patterns, loss, in-situ and ex-situ conservation", 15);
            t(c, "Biodiversity – Types and Patterns", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Loss of Biodiversity and Causes", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Biodiversity Conservation – Methods", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(bio, "Environmental Issues", "Air/water pollution, deforestation, ozone depletion", 16);
            t(c, "Air Pollution and its Control", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Water Pollution and Eutrophication", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Deforestation and Ozone Depletion", 1.0, Topic.DifficultyLevel.MEDIUM, 3);

        // ── English Core ─────────────────────────────────────────────────────
        Subject eng = sub(exam, "English Core", "CBSE Class 12 English – Flamingo, Vistas and Writing Skills", "menu_book", "#6A1B9A", 5);
        c = ch(eng, "Flamingo – Prose", "Prose chapters from Flamingo", 1);
            t(c, "The Last Lesson", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Lost Spring", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Deep Water", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "The Rattrap", 1.0, Topic.DifficultyLevel.MEDIUM, 4);
            t(c, "Indigo", 1.0, Topic.DifficultyLevel.MEDIUM, 5);
            t(c, "Poets and Pancakes", 1.0, Topic.DifficultyLevel.MEDIUM, 6);
            t(c, "The Interview", 1.0, Topic.DifficultyLevel.EASY, 7);
            t(c, "Going Places", 1.0, Topic.DifficultyLevel.MEDIUM, 8);
        c = ch(eng, "Flamingo – Poetry", "Poems from Flamingo", 2);
            t(c, "My Mother at Sixty-six", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Keeping Quiet", 0.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "A Thing of Beauty", 0.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "A Roadside Stand", 0.5, Topic.DifficultyLevel.MEDIUM, 4);
            t(c, "Aunt Jennifer's Tigers", 0.5, Topic.DifficultyLevel.MEDIUM, 5);
        c = ch(eng, "Vistas – Supplementary Reader", "Chapters from Vistas", 3);
            t(c, "The Third Level", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "The Tiger King", 1.0, Topic.DifficultyLevel.EASY, 2);
            t(c, "Journey to the End of the Earth", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "The Enemy", 1.0, Topic.DifficultyLevel.MEDIUM, 4);
            t(c, "Should Wizard Hit Mommy?", 1.0, Topic.DifficultyLevel.MEDIUM, 5);
            t(c, "On the Face of It", 1.0, Topic.DifficultyLevel.MEDIUM, 6);
            t(c, "Evans Tries an O-Level", 1.0, Topic.DifficultyLevel.HARD, 7);
            t(c, "Memories of Childhood", 1.0, Topic.DifficultyLevel.MEDIUM, 8);
        c = ch(eng, "Writing Skills", "Notice, letter, advertisement, article, report, speech", 4);
            t(c, "Notice Writing", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Formal Letter – Letter of Enquiry/Complaint", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Article and Speech Writing", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
            t(c, "Report Writing", 1.5, Topic.DifficultyLevel.HARD, 4);

        // ── Accountancy ──────────────────────────────────────────────────────
        Subject acc = sub(exam, "Accountancy", "CBSE Class 12 Accountancy – Partnership, Company Accounts, Financial Statements", "account_balance", "#F57F17", 6);
        c = ch(acc, "Accounting for Partnership – Basic Concepts", "Partnership deed, capital accounts, profit sharing", 1);
            t(c, "Nature of Partnership and Partnership Deed", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Fixed and Fluctuating Capital Accounts", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Distribution of Profit – Appropriation Account", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(acc, "Reconstitution – Admission of a Partner", "Goodwill, revaluation, capital adjustment", 2);
            t(c, "Sacrificing Ratio and Goodwill", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "Revaluation of Assets and Liabilities", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Capital Adjustment on Admission", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(acc, "Reconstitution – Retirement and Death of a Partner", "Gaining ratio, executor's account", 3);
            t(c, "Gaining Ratio and Goodwill on Retirement", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "Revaluation and Capital Settlement", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Death of a Partner – Executor's Account", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(acc, "Dissolution of Partnership Firm", "Realisation account, settlement of accounts", 4);
            t(c, "Modes of Dissolution", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Realisation Account", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Settlement of Accounts on Dissolution", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(acc, "Accounting for Share Capital", "Issue, forfeiture and reissue of shares", 5);
            t(c, "Share Capital and Types of Shares", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Issue of Shares at Par, Premium and Discount", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Forfeiture and Reissue of Shares", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(acc, "Issue and Redemption of Debentures", "Issue at par/premium/discount, redemption methods", 6);
            t(c, "Issue of Debentures", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "Interest on Debentures", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Redemption of Debentures", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(acc, "Financial Statements of a Company", "P&L account, balance sheet, adjustments", 7);
            t(c, "Statement of Profit and Loss", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "Balance Sheet of a Company", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Adjustments in Financial Statements", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(acc, "Analysis of Financial Statements", "Comparative, common-size statements", 8);
            t(c, "Objectives and Tools of Analysis", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Comparative Financial Statements", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Common-Size Financial Statements", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(acc, "Accounting Ratios", "Liquidity, solvency, profitability, activity ratios", 9);
            t(c, "Liquidity Ratios – Current and Quick Ratio", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Solvency and Coverage Ratios", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Profitability Ratios", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(acc, "Cash Flow Statement", "Operating, investing, financing activities (Indirect Method)", 10);
            t(c, "Cash Flow from Operating Activities", 2.0, Topic.DifficultyLevel.HARD, 1);
            t(c, "Cash Flow from Investing Activities", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Cash Flow from Financing Activities", 1.5, Topic.DifficultyLevel.HARD, 3);

        // ── Business Studies ──────────────────────────────────────────────────
        Subject bs = sub(exam, "Business Studies", "CBSE Class 12 Business Studies – Principles and Functions of Management, Business Finance and Marketing", "business_center", "#E64A19", 7);
        c = ch(bs, "Nature and Significance of Management", "Concept, features, functions, levels", 1);
            t(c, "Concept and Characteristics of Management", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Levels and Functions of Management", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Management as a Profession and Science/Art", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(bs, "Principles of Management", "Fayol's 14 principles, Taylor's scientific management", 2);
            t(c, "Fayol's 14 Principles of Management", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Taylor's Scientific Management", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
        c = ch(bs, "Business Environment", "Dimensions, impact, Demonetisation, liberalisation, GST", 3);
            t(c, "Concept and Dimensions of Business Environment", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Liberalisation, Privatisation and Globalisation", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
        c = ch(bs, "Planning", "Importance, process, types of plans, limitations", 4);
            t(c, "Concept and Importance of Planning", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Planning Process and Types of Plans", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Limitations of Planning", 0.5, Topic.DifficultyLevel.EASY, 3);
        c = ch(bs, "Organising", "Delegation, decentralisation, formal and informal organisations", 5);
            t(c, "Organising Process and Importance", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Delegation and Decentralisation", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Formal and Informal Organisation", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(bs, "Staffing", "Recruitment, selection, training, performance appraisal", 6);
            t(c, "Importance and Process of Staffing", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Recruitment and Selection", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Training and Development", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(bs, "Directing", "Motivation, leadership, communication, supervision", 7);
            t(c, "Concept and Elements of Directing", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Motivation – Maslow's and Herzberg's Theory", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Leadership Styles and Communication", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(bs, "Controlling", "Process, relationship with planning, techniques", 8);
            t(c, "Importance and Process of Controlling", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Techniques of Managerial Control", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
        c = ch(bs, "Financial Management", "Capital structure, financial planning, working capital", 9);
            t(c, "Financial Planning and Capital Structure", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Fixed and Working Capital", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Factors Affecting Capital Structure", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(bs, "Financial Markets", "Money market, capital market, NSE, BSE, SEBI", 10);
            t(c, "Money Market Instruments", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Capital Market – Primary and Secondary", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "NSE, BSE and SEBI", 1.0, Topic.DifficultyLevel.EASY, 3);
        c = ch(bs, "Marketing Management", "Marketing mix, product, price, promotion, place", 11);
            t(c, "Marketing Concept and Functions", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "Marketing Mix – 4 Ps", 2.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Advertising vs Personal Selling", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(bs, "Consumer Protection", "Consumer rights, COPRA 1986, redressal agencies", 12);
            t(c, "Importance of Consumer Protection", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Consumer Rights and Responsibilities", 1.0, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Consumer Protection Act 1986 and Redressal Forums", 1.5, Topic.DifficultyLevel.MEDIUM, 3);

        // ── Economics ────────────────────────────────────────────────────────
        Subject eco = sub(exam, "Economics", "CBSE Class 12 Economics – Microeconomics and Macroeconomics", "trending_up", "#00695C", 8);
        // Microeconomics
        c = ch(eco, "Introduction to Microeconomics", "Central problems, opportunity cost, production possibility curve", 1);
            t(c, "Central Problems of an Economy", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Production Possibility Curve and Opportunity Cost", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
        c = ch(eco, "Theory of Consumer Behaviour", "Utility, indifference curves, budget line, consumer equilibrium", 2);
            t(c, "Utility Analysis and Law of Diminishing Marginal Utility", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Indifference Curve Analysis", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Budget Set and Consumer Equilibrium", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(eco, "Production and Costs", "Production function, law of variable proportions, returns to scale, cost curves", 3);
            t(c, "Production Function and Total/Marginal/Average Product", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Returns to a Factor and Returns to Scale", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Cost – Total, Marginal and Average Cost Curves", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(eco, "The Theory of the Firm – Perfect Competition", "Demand curve, revenue, profit maximisation", 4);
            t(c, "Revenue Curves Under Perfect Competition", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Profit Maximisation and Break-even", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Supply Curve and Producer Equilibrium", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(eco, "Market Equilibrium", "Demand and supply, price determination, floor and ceiling prices", 5);
            t(c, "Equilibrium Price and Quantity", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Shifts in Demand and Supply", 1.5, Topic.DifficultyLevel.MEDIUM, 2);
            t(c, "Price Floor and Price Ceiling", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(eco, "Non-Competitive Markets", "Monopoly, monopolistic competition, oligopoly", 6);
            t(c, "Monopoly – Features and Pricing", 1.5, Topic.DifficultyLevel.HARD, 1);
            t(c, "Monopolistic Competition", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Oligopoly – Features", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
        // Macroeconomics
        c = ch(eco, "National Income Accounting", "GDP, GNP, NNP, real and nominal, circular flow", 7);
            t(c, "Circular Flow of Income", 1.0, Topic.DifficultyLevel.EASY, 1);
            t(c, "GDP, GNP, NNP and NI – Concepts and Formulae", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Methods of Measuring National Income", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(eco, "Money and Banking", "Functions of money, commercial banks, RBI, money multiplier", 8);
            t(c, "Money – Types and Functions", 0.5, Topic.DifficultyLevel.EASY, 1);
            t(c, "Commercial Banks – Functions and Credit Creation", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Reserve Bank of India – Functions", 1.5, Topic.DifficultyLevel.MEDIUM, 3);
        c = ch(eco, "Determination of Income and Employment", "Aggregate demand, multiplier, full employment", 9);
            t(c, "Aggregate Demand and Its Components", 1.5, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Consumption Function and Saving Function", 2.0, Topic.DifficultyLevel.HARD, 2);
            t(c, "Investment Multiplier and Excess/Deficient Demand", 2.0, Topic.DifficultyLevel.HARD, 3);
        c = ch(eco, "Government Budget and the Economy", "Revenue and capital budget, fiscal policy, deficit", 10);
            t(c, "Government Budget – Components and Objectives", 1.0, Topic.DifficultyLevel.MEDIUM, 1);
            t(c, "Measures of Government Deficit", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Fiscal Policy and its Effects", 1.5, Topic.DifficultyLevel.HARD, 3);
        c = ch(eco, "Open Economy Macroeconomics", "Balance of payments, foreign exchange rate", 11);
            t(c, "Balance of Payments – Components", 1.5, Topic.DifficultyLevel.HARD, 1);
            t(c, "Foreign Exchange Rate – Determination", 1.5, Topic.DifficultyLevel.HARD, 2);
            t(c, "Managed Float and Fixed Exchange Rate", 1.0, Topic.DifficultyLevel.MEDIUM, 3);
    }
}
