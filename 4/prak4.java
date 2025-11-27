import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public class prak4 {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("=== Початок виконання лабораторної роботи ===\n");

        CompletableFuture<Void> startInfo = CompletableFuture.runAsync(() -> {
            System.out.println("[Info] Запуск асинхронних процесiв...");
        });
        startInfo.get();

        System.out.println("\n--- ЗАВДАННЯ 1 (Матриця 3x3) ---");
        performTask1();

        System.out.println("\n--------------------------------");
        System.out.println("--- ЗАВДАННЯ 2 (Послiдовнiсть чисел) ---");
        performTask2();

        try {
            Thread.sleep(2000); 
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("\n=== Робота завершена ===");
    }

    private static void performTask1() throws ExecutionException, InterruptedException {
        long startTime = System.nanoTime();

        CompletableFuture<int[][]> matrixFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("[Task 1] Генерацiя матрицi почалась...");
            int[][] matrix = new int[3][3];
            Random random = new Random();
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    matrix[i][j] = random.nextInt(100);
                }
            }
            long genTime = System.nanoTime() - startTime;
            System.out.printf("[Task 1] Матриця згенерована за: %d нс%n", genTime);
            return matrix;
        });

        matrixFuture.thenAcceptAsync(matrix -> {
            System.out.println("[Task 1] Початкова матриця:");
            for (int[] row : matrix) {
                for (int val : row) {
                    System.out.print(val + "\t");
                }
                System.out.println();
            }
        });

        CompletableFuture<Void> printColumnsFuture = matrixFuture.thenAcceptAsync(matrix -> {
            long colStart = System.nanoTime();
            System.out.println("[Task 1] Виведення стовпчикiв:");
            
            for (int col = 0; col < 3; col++) {
                StringBuilder sb = new StringBuilder();
                sb.append("Стовпець ").append(col + 1).append(": ");
                for (int row = 0; row < 3; row++) {
                    sb.append(matrix[row][col]).append(row < 2 ? ", " : "");
                }
                System.out.println(sb.toString());
            }
            long colTime = System.nanoTime() - colStart;
            System.out.printf("[Task 1] Час виведення стовпчикiв: %d нс%n", colTime);
        });

        printColumnsFuture.thenRunAsync(() -> {
            long endTime = System.nanoTime();
            System.out.println("[Task 1] (thenRunAsync) Всi операцiї по матрицi завершено!");
            System.out.printf("[Task 1] Загальний час виконання задачi 1: %d нс%n", (endTime - startTime));
        }).join();
    }

    private static void performTask2() {
        long startTotal = System.nanoTime();

        CompletableFuture<List<Double>> sequenceFuture = CompletableFuture.supplyAsync(() -> {
            List<Double> list = new ArrayList<>();
            System.out.println("[Task 2] Генерацiя послiдовностi...");
            for (int i = 0; i < 20; i++) {
                list.add(Math.round(ThreadLocalRandom.current().nextDouble(100.0) * 100.0) / 100.0);
            }
            
            System.out.println("[Task 2] Згенерована послiдовнiсть:");
            System.out.println(list);
            return list;
        });

        CompletableFuture<Double> calculationFuture = sequenceFuture.thenApplyAsync(list -> {
            double minOddIndex = Double.MAX_VALUE;
            double maxEvenIndex = Double.MIN_VALUE;

            for (int i = 0; i < list.size(); i++) {
                double val = list.get(i);
                if (i % 2 == 0) {
                    if (val < minOddIndex) minOddIndex = val;
                } else {
                    if (val > maxEvenIndex) maxEvenIndex = val;
                }
            }

            System.out.printf("[Task 2 Info] Min(a1, a3...): %.2f%n", minOddIndex);
            System.out.printf("[Task 2 Info] Max(a2, a4...): %.2f%n", maxEvenIndex);

            return minOddIndex + maxEvenIndex;
        });

        calculationFuture.thenAcceptAsync(result -> {
            System.out.printf("[Task 2 RESULT] Результат обчислення: %.2f%n", result);
        }).thenRun(() -> {
            long totalTime = System.nanoTime() - startTotal;
            System.out.printf("[Task 2] Час роботи всiх асинхронних операцiй: %d нс%n", totalTime);
        }).join();
    }
}