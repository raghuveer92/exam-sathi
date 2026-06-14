package com.examsaathi.service;

import com.examsaathi.config.GoogleSheetsProperties;
import com.examsaathi.dto.sheet.SheetQuestion;
import com.examsaathi.exception.GoogleSheetsException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleSheetsQuestionLoaderTest {

    private final GoogleSheetsQuestionLoader loader =
        new GoogleSheetsQuestionLoader(new GoogleSheetsProperties());

    @Test
    void parsesExpectedQuestionSheetFormat() {
        String csv = String.join("\n",
            "id,exam,subject,topic,question_en,question_hi,optionA_en,optionA_hi,optionB_en,optionB_hi,optionC_en,optionC_hi,optionD_en,optionD_hi,correct_option,difficulty,explanation_en,explanation_hi,tags",
            "SSC-CGL-T241-Q01,SSC CGL,English,Vocabularies,\"Synonym of wealthy?\",धनी का समानार्थी?,Poor,गरीब,Rich,धनी,Hungry,भूखा,Average,साधारण,b,easy,Wealthy means rich.,Wealthy का अर्थ rich होता है।,ssc-cgl;english;vocabularies;easy"
        );

        List<SheetQuestion> questions = loader.parseCsv(csv);

        assertThat(questions).hasSize(1);
        SheetQuestion q = questions.get(0);
        assertThat(q.getId()).isEqualTo("SSC-CGL-T241-Q01");
        assertThat(q.getExam()).isEqualTo("SSC CGL");
        assertThat(q.getSubject()).isEqualTo("English");
        assertThat(q.getTopic()).isEqualTo("Vocabularies");
        assertThat(q.getQuestionEn()).isEqualTo("Synonym of wealthy?");
        assertThat(q.getQuestionHi()).isEqualTo("धनी का समानार्थी?");
        assertThat(q.getOptionAEn()).isEqualTo("Poor");
        assertThat(q.getOptionAHi()).isEqualTo("गरीब");
        assertThat(q.getOptionBEn()).isEqualTo("Rich");
        assertThat(q.getOptionBHi()).isEqualTo("धनी");
        assertThat(q.getOptionCEn()).isEqualTo("Hungry");
        assertThat(q.getOptionCHi()).isEqualTo("भूखा");
        assertThat(q.getOptionDEn()).isEqualTo("Average");
        assertThat(q.getOptionDHi()).isEqualTo("साधारण");
        assertThat(q.getCorrectOption()).isEqualTo("B");
        assertThat(q.getDifficulty()).isEqualTo("EASY");
        assertThat(q.getExplanationEn()).isEqualTo("Wealthy means rich.");
        assertThat(q.getExplanationHi()).isEqualTo("Wealthy का अर्थ rich होता है।");
        assertThat(q.getTags()).isEqualTo("ssc-cgl;english;vocabularies;easy");
    }

    @Test
    void rejectsRowsMissingMandatoryHindiAndExplanationFields() {
        String csv = String.join("\n",
            "id,exam,subject,topic,question_en,question_hi,optionA_en,optionA_hi,optionB_en,optionB_hi,optionC_en,optionC_hi,optionD_en,optionD_hi,correct_option,difficulty,explanation_en,explanation_hi,tags",
            "SSC-CGL-T241-Q01,SSC CGL,English,Vocabularies,\"Synonym of wealthy?\",,Poor,गरीब,Rich,धनी,Hungry,भूखा,Average,साधारण,B,EASY,Wealthy means rich.,,ssc-cgl;english;vocabularies;easy"
        );

        assertThatThrownBy(() -> loader.parseCsv(csv))
            .isInstanceOf(GoogleSheetsException.class)
            .hasMessageContaining("question_hi")
            .hasMessageContaining("explanation_hi");
    }

    @Test
    void rejectsInvalidCorrectOptionAndDifficulty() {
        String csv = String.join("\n",
            "id,exam,subject,topic,question_en,question_hi,optionA_en,optionA_hi,optionB_en,optionB_hi,optionC_en,optionC_hi,optionD_en,optionD_hi,correct_option,difficulty,explanation_en,explanation_hi,tags",
            "SSC-CGL-T241-Q01,SSC CGL,English,Vocabularies,\"Synonym of wealthy?\",धनी का समानार्थी?,Poor,गरीब,Rich,धनी,Hungry,भूखा,Average,साधारण,E,BASIC,Wealthy means rich.,Wealthy का अर्थ rich होता है।,ssc-cgl;english;vocabularies;easy"
        );

        assertThatThrownBy(() -> loader.parseCsv(csv))
            .isInstanceOf(GoogleSheetsException.class)
            .hasMessageContaining("invalid correct_option");
    }
}
