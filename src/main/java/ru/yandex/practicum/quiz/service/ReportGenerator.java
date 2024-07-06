package ru.yandex.practicum.quiz.service;


import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.quiz.config.AppConfig;
import ru.yandex.practicum.quiz.config.AppConfig.ReportMode;
import ru.yandex.practicum.quiz.config.AppConfig.ReportOutputMode;
import ru.yandex.practicum.quiz.config.AppConfig.ReportOutputSettings;
import ru.yandex.practicum.quiz.config.AppConfig.ReportSettings;
import ru.yandex.practicum.quiz.model.QuizLog;

import java.io.PrintWriter;
import java.util.List;
import ru.yandex.practicum.quiz.model.QuizLog.Entry;
@Slf4j
@Component
public class ReportGenerator {

  private final String reportTitle;
  private final ReportSettings reportSettings;

  public ReportGenerator(AppConfig appConfig) {
    this.reportTitle = appConfig.getTitle();
    this.reportSettings = appConfig.getReport();
  }

  public void generate(QuizLog quizLog) {
    //when report generation is off then finish process
    if (!reportSettings.isEnabled()) {
      log.debug("Report output is disabled, report generation has been stopped.");
      return;
    }
    ReportOutputSettings outputSettings = reportSettings.getOutput();
    log.trace("Report will be generated: {}", outputSettings.getMode());
    try {
      // Создаём объект PrintWriter, выводящий отчет в консоль or to the file based on settings
      boolean isConsole = outputSettings.getMode().equals(ReportOutputMode.CONSOLE);
      try (PrintWriter writer = (isConsole ?
          new PrintWriter(System.out) : //output to the Console
          new PrintWriter(outputSettings.getPath()))) {
        // записываем отчет
        write(quizLog, writer);
      }
    } catch (Exception exception) {
      log.warn("An error occurred while generating the report: ", exception);
    }
  }

  private void write(QuizLog quizLog, PrintWriter writer) {
    writer.printf("Отчет о прохождении теста %s.\n", reportTitle);
    for (QuizLog.Entry entry : quizLog) {
      if (reportSettings.getMode().equals(ReportMode.VERBOSE)) {
        writeVerbose(writer, entry);
      } else {
        writeConcise(writer, entry);
      }
    }
    writer.printf("Всего вопросов: %d\nОтвечено правильно: %d\n", quizLog.total(),
        quizLog.successful());
  }

  private void writeVerbose(PrintWriter writer, QuizLog.Entry entry) {
    // Записываем номер вопроса и текст вопроса
    writer.println("Вопрос " + entry.getNumber() + ": " + entry.getQuestion().getText());
    // Записываем варианты ответов
    List<String> options = entry.getQuestion().getOptions();
    for (int i = 0; i < options.size(); i++) {
      writer.println((i + 1) + ") " + options.get(i));
    }
    // Записываем ответы пользователя
    writer.print("Ответы пользователя: ");
    List<Integer> answers = entry.getAnswers();
    for (Integer answer : answers) {
      writer.print(answer + " ");
    }
    writer.println();
    //Записываем флаг успешности ответа
    String successFlag = entry.isSuccessful() ? "да" : "нет";
    writer.println("Содержит правильный ответ: " + successFlag);
    // Добавляем пустую строку между записями
    writer.println();
  }

  private void writeConcise(PrintWriter writer, QuizLog.Entry entry) {
    // Записываем номер вопроса и текст вопроса
    char successSign = entry.isSuccessful() ? '+' : '-';
    String answers = entry.getAnswers().stream()
        .map(Object::toString)
        .collect(Collectors.joining(","));

    writer.printf("%d(%s): %s\n", entry.getNumber(), successSign, answers);
  }
}

