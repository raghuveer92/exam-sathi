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

        seedCbse10(cbse10);
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
}
