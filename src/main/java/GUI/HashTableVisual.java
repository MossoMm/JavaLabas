package GUI;

import WareHouse.HashTable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;


public class HashTableVisual {

    private final Canvas canvas;

    // Цвета ячеек
    private static final Color COLOR_EMPTY     = Color.web("#1f2937");  // тёмно-серый
    private static final Color COLOR_OCCUPIED  = Color.web("#10b981");  // зелёный
    private static final Color COLOR_TOMBSTONE = Color.web("#6b7280");  // серый
    private static final Color COLOR_PROBED    = Color.web("#fbbf24");  // жёлтый
    private static final Color COLOR_TEXT      = Color.WHITE;
    private static final Color COLOR_INDEX     = Color.web("#9ca3af");

    private static final double CELL_HEIGHT = 60;
    private static final double CELL_PADDING = 4;
    private static final double HEADER_HEIGHT = 30;

    public HashTableVisual(Canvas canvas) {
        this.canvas = canvas;
    }


    //Нарисовать хеш-таблицу.

    public void draw(HashTable.TableSnapshot snapshot, int[] probedIndices) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        int capacity = snapshot.keys.length;

        // Вычисляем размеры ячеек
        double cellWidth = (canvas.getWidth() - CELL_PADDING) / capacity;
        cellWidth = Math.min(cellWidth, 100); // максимум 100px

        // Очищаем canvas
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Рисуем заголовок
        g.setFill(COLOR_TEXT);
        g.setFont(Font.font("Consolas", 12));
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText(String.format("capacity=%d  size=%d  load=%.2f",
                        capacity, snapshot.size,
                        capacity == 0 ? 0 : (double) snapshot.size / capacity),
                10, 15);

        // Рисуем ячейки
        for (int i = 0; i < capacity; i++) {
            double x = i * cellWidth + CELL_PADDING / 2;
            double y = HEADER_HEIGHT;

            // Определяем цвет ячейки
            Color fillColor = getCellColor(i, snapshot, probedIndices);

            // Рисуем прямоугольник
            g.setFill(fillColor);
            g.fillRoundRect(x, y, cellWidth - CELL_PADDING, CELL_HEIGHT, 6, 6);

            // Обводка
            g.setStroke(Color.web("#374151"));
            g.setLineWidth(1);
            g.strokeRoundRect(x, y, cellWidth - CELL_PADDING, CELL_HEIGHT, 6, 6);

            // Индекс сверху
            g.setFill(COLOR_INDEX);
            g.setFont(Font.font("Consolas", 9));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(String.valueOf(i), x + (cellWidth - CELL_PADDING) / 2, y - 3);

            // Содержимое ячейки
            String content = getCellContent(i, snapshot);
            g.setFill(COLOR_TEXT);
            g.setFont(Font.font("Consolas", 10));
            g.fillText(content, x + (cellWidth - CELL_PADDING) / 2, y + CELL_HEIGHT / 2 + 4);
        }

        // Легенда
        drawLegend(g, snapshot.keys.length * cellWidth + 20);
    }

    private Color getCellColor(int idx, HashTable.TableSnapshot snapshot, int[] probedIndices) {
        // Приоритет 1: посещённая ячейка (жёлтый)
        if (probedIndices!=null)
        {
            for (int p : probedIndices) {
                if (p == idx) return COLOR_PROBED;
            }
        }


        // Приоритет 2: tombstone (серый)
        if (snapshot.keys[idx] == null && snapshot.deleted[idx]) {
            return COLOR_TOMBSTONE;
        }

        // Приоритет 3: занятая (зелёный)
        if (snapshot.keys[idx] != null) {
            return COLOR_OCCUPIED;
        }

        // По умолчанию: пустая
        return COLOR_EMPTY;
    }

    private String getCellContent(int idx, HashTable.TableSnapshot snapshot) {
        if (snapshot.keys[idx] != null) {
            String key = snapshot.keys[idx].toString();
            // Обрезаем длинные ключи
            return key.length() > 10 ? key.substring(0, 9) + "…" : key;
        }
        if (snapshot.deleted[idx]) {
            return "x";
        }
        return "—";
    }

    private void drawLegend(GraphicsContext g, double startX) {
        double x = 10;
        double y = HEADER_HEIGHT + CELL_HEIGHT + 20;

        g.setFont(Font.font("Consolas", 10));
        g.setTextAlign(TextAlignment.LEFT);

        // Пустая
        g.setFill(COLOR_EMPTY);
        g.fillRect(x, y, 15, 15);
        g.setFill(COLOR_TEXT);
        g.fillText("Пустая", x + 20, y + 12);
        x += 90;

        // Занятая
        g.setFill(COLOR_OCCUPIED);
        g.fillRect(x, y, 15, 15);
        g.setFill(COLOR_TEXT);
        g.fillText("Занятая", x + 20, y + 12);
        x += 95;

        // Tombstone
        g.setFill(COLOR_TOMBSTONE);
        g.fillRect(x, y, 15, 15);
        g.setFill(COLOR_TEXT);
        g.fillText("Tombstone", x + 20, y + 12);
        x += 115;

        // Посещённая
        g.setFill(COLOR_PROBED);
        g.fillRect(x, y, 15, 15);
        g.setFill(COLOR_TEXT);
        g.fillText("Посещённая", x + 20, y + 12);
    }


    public void clear() {
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
}