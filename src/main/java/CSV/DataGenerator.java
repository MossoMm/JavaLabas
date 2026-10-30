package CSV;

import PartyModel.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Генератор случайных партий для тестирования и замеров производительности.
 * Требование ЛР №2: "Генерация набора данных" — возможность создать
 * большое количество элементов (10⁴, 10⁵) для тестов и бенчмарков.
 *
 * Поддерживает:
 * - Генерацию одной случайной партии
 * - Генерацию списка из N партий
 * - Генерацию с возможностью отмены (для фоновых Task)
 * - Разные типы партий: RegularParty, ArchiveParty, ImportParty
 */
public final class DataGenerator {

    private DataGenerator() {
        // утилитарный класс — нельзя создать экземпляр
    }

    private static final Random RANDOM = new Random();


    // ПУЛЫ СЛУЧАЙНЫХ ДАННЫХ

    private static final String[] ARTICLE_PREFIXES = {
            "ART", "SKU", "PRD", "ITM", "MOD"
    };

    private static final String[] NAME_PREFIXES = {
            "Деталь ", "Модуль ", "Блок ", "Узел ", "Компонент ",
            "Элемент ", "Механизм ", "Агрегат ", "Секция ", "Панель ",
            "Корпус ", "Регулятор ", "Датчик ", "Привод ", "Клапан "
    };

    private static final String[] NAME_SUFFIXES = {
            "А", "Б", "В", "Г", "Д", "Е", "Ж", "З", "И", "К",
            "Л", "М", "Н", "П", "Р"
    };

    private static final String[] CELL_ROWS = {
            "A", "B", "C", "D", "E", "F", "G", "H"
    };

    private static final String[] COUNTRIES = {
            "Россия", "Китай", "Германия", "Япония", "США",
            "Италия", "Франция", "Корея", "Индия", "Бразилия",
            "Великобритания", "Канада", "Австралия", "Польша", "Турция"
    };

    private static final String[] CUSTOMS_CODES = {
            "10200000", "53010000", "84710000", "39020000",
            "72080000", "85420000", "61100000", "95030000",
            "22040000", "33040000", "48020000", "62040000",
            "73080000", "84810000", "94030000"
    };



    /**
     * Сгенерировать одну случайную партию.
     * Распределение типов:
     *   60% — RegularParty
     *   20% — ArchiveParty
     *   20% — ImportParty
     */
    public static Party generateRandomParty(int index) {
        String article = generateArticle(index);
        String name = generateName();
        int quantity = generateQuantity();
        String cell = generateCell();
        LocalDate date = generateDate();

        double typeRoll = RANDOM.nextDouble();
        if (typeRoll < 0.6) {
            // RegularParty (60%)
            return new RegularParty(article, name, quantity, cell, date);
        } else if (typeRoll < 0.8) {
            // ArchiveParty (20%)
            return new ArchiveParty(article, name, quantity, cell, date);
        } else {
            // ImportParty (20%)
            String country = COUNTRIES[RANDOM.nextInt(COUNTRIES.length)];
            String customs = CUSTOMS_CODES[RANDOM.nextInt(CUSTOMS_CODES.length)];
            return new ImportParty(article, name, quantity, cell, date, country, customs);
        }
    }

    /**
     * Сгенерировать список из N случайных партий.
     */
    public static List<Party> generateParties(int count) {
        List<Party> parties = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            parties.add(generateRandomParty(i));
        }
        return parties;
    }


    public static List<Party> generatePartiesWithCancel(
            int count, java.util.function.BooleanSupplier isCancelled) {
        List<Party> parties = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (isCancelled.getAsBoolean()) {
                return parties; // вернём то, что успели сгенерировать
            }
            parties.add(generateRandomParty(i));
        }
        return parties;
    }

    /**
     * Сгенерировать список партий с отменой и обновлением прогресса.
     */
    public static List<Party> generatePartiesWithProgress(
            int count,
            java.util.function.BooleanSupplier isCancelled,
            java.util.function.IntConsumer onProgress) {
        List<Party> parties = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (isCancelled.getAsBoolean()) {
                return parties;
            }
            parties.add(generateRandomParty(i));
            onProgress.accept(i + 1);
        }
        return parties;
    }




    private static String generateArticle(int index) {
        String prefix = ARTICLE_PREFIXES[RANDOM.nextInt(ARTICLE_PREFIXES.length)];
        return String.format("%s-%05d", prefix, index + 1);
    }

    private static String generateName() {
        String prefix = NAME_PREFIXES[RANDOM.nextInt(NAME_PREFIXES.length)];
        String suffix = NAME_SUFFIXES[RANDOM.nextInt(NAME_SUFFIXES.length)];
        return prefix + " " + suffix;
    }

    private static int generateQuantity() {
        // Распределение: большинство партий имеют небольшое количество
        // 50% — от 1 до 50
        // 30% — от 51 до 150
        // 20% — от 151 до 500
        double roll = RANDOM.nextDouble();
        if (roll < 0.5) {
            return RANDOM.nextInt(50) + 1;
        } else if (roll < 0.8) {
            return RANDOM.nextInt(100) + 51;
        } else {
            return RANDOM.nextInt(350) + 151;
        }
    }

    private static String generateCell() {
        String row = CELL_ROWS[RANDOM.nextInt(CELL_ROWS.length)];
        int col = RANDOM.nextInt(10) + 1;
        int shelf = RANDOM.nextInt(5) + 1;
        return String.format("%s-%d-%d", row, col, shelf);
    }

    private static LocalDate generateDate() {
        // Дата в диапазоне последних 3 лет
        int daysAgo = RANDOM.nextInt(1095);
        return LocalDate.now().minusDays(daysAgo);
    }
}