import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

public class Lab3 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== МЕНЮ ЛАБОРАТОРНОЇ РОБОТИ ===");
            System.out.println("1. Завдання 1: Множення матриць (Work Stealing vs Work Dealing)");
            System.out.println("2. Завдання 2: Пошук файлiв у директорiї (Work Stealing)");
            System.out.println("0. Вихiд");
            System.out.print("Ваш вибiр: ");
            
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    runMatrixTask(scanner);
                    break;
                case "2":
                    runFileTask(scanner);
                    break;
                case "0":
                    System.exit(0);
                default:
                    System.out.println("Невiрний вибiр.");
            }
        }
    }

    // ==========================================
    // ЧАСТИНА 1: МАТРИЦi
    // ==========================================
    private static void runMatrixTask(Scanner scanner) {
        System.out.println("\n--- Множення матриць ---");
        System.out.print("Введiть кiлькiсть рядкiв матрицi A: ");
        int rowsA = Integer.parseInt(scanner.nextLine());
        System.out.print("Введiть кiлькiсть стовпцiв матрицi A (та рядкiв B): ");
        int colsA = Integer.parseInt(scanner.nextLine());
        System.out.print("Введiть кiлькiсть стовпцiв матрицi B: ");
        int colsB = Integer.parseInt(scanner.nextLine());

        // Генерацiя матриць
        int[][] matrixA = generateMatrix(rowsA, colsA);
        int[][] matrixB = generateMatrix(colsA, colsB);

        System.out.println("Матрицi згенеровано.");
        if (rowsA <= 10 && colsB <= 10) { // Виводимо тiльки якщо малi
            System.out.println("Матриця A:");
            printMatrix(matrixA);
            System.out.println("Матриця B:");
            printMatrix(matrixB);
        }

        // 1. WORK DEALING (ExecutorService - Thread Pool)
        System.out.println("\nЗапуск Work Dealing (ExecutorService)...");
        long startDealing = System.nanoTime();
        int[][] resultDealing = multiplyWorkDealing(matrixA, matrixB);
        long endDealing = System.nanoTime();
        double timeDealing = (endDealing - startDealing) / 1_000_000.0;
        System.out.println("⏱️ Час Work Dealing: " + timeDealing + " мс");

        // 2. WORK STEALING (ForkJoinPool)
        System.out.println("\nЗапуск Work Stealing (ForkJoin Framework)...");
        long startStealing = System.nanoTime();
        int[][] resultStealing = multiplyWorkStealing(matrixA, matrixB);
        long endStealing = System.nanoTime();
        double timeStealing = (endStealing - startStealing) / 1_000_000.0;
        System.out.println("⏱️ Час Work Stealing: " + timeStealing + " мс");
        
        // Вивiд результату (опцiонально)
        if (rowsA <= 10 && colsB <= 10) {
            System.out.println("Результат:");
            printMatrix(resultStealing);
        }
    }

    // --- Логiка Work Dealing ---
    private static int[][] multiplyWorkDealing(int[][] A, int[][] B) {
        int rowsA = A.length;
        int colsB = B[0].length;
        int[][] C = new int[rowsA][colsB];
        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        // Розбиваємо роботу по рядках
        for (int i = 0; i < rowsA; i++) {
            final int rowIdx = i;
            executor.submit(() -> {
                for (int j = 0; j < colsB; j++) {
                    for (int k = 0; k < A[0].length; k++) {
                        C[rowIdx][j] += A[rowIdx][k] * B[k][j];
                    }
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return C;
    }

    // --- Логiка Work Stealing ---
    private static int[][] multiplyWorkStealing(int[][] A, int[][] B) {
        int rowsA = A.length;
        int colsB = B[0].length;
        int[][] C = new int[rowsA][colsB];
        
        ForkJoinPool pool = new ForkJoinPool();
        // Запускаємо рекурсивну задачу для обробки рядкiв вiд 0 до rowsA
        pool.invoke(new MatrixRecursiveTask(A, B, C, 0, rowsA));
        return C;
    }

    // Рекурсивна задача для ForkJoin
    static class MatrixRecursiveTask extends RecursiveAction {
        private static final int THRESHOLD = 64; // Порiг розбиття задачi
        private int[][] A, B, C;
        private int startRow, endRow;

        public MatrixRecursiveTask(int[][] A, int[][] B, int[][] C, int startRow, int endRow) {
            this.A = A; this.B = B; this.C = C;
            this.startRow = startRow; this.endRow = endRow;
        }

        @Override
        protected void compute() {
            // Якщо дiапазон малий - рахуємо прямо
            if ((endRow - startRow) <= THRESHOLD) {
                for (int i = startRow; i < endRow; i++) {
                    for (int j = 0; j < B[0].length; j++) {
                        for (int k = 0; k < A[0].length; k++) {
                            C[i][j] += A[i][k] * B[k][j];
                        }
                    }
                }
            } else {
                // Якщо дiапазон великий - дiлимо навпiл (Work Stealing)
                int mid = (startRow + endRow) / 2;
                MatrixRecursiveTask left = new MatrixRecursiveTask(A, B, C, startRow, mid);
                MatrixRecursiveTask right = new MatrixRecursiveTask(A, B, C, mid, endRow);
                invokeAll(left, right);
            }
        }
    }

    // Допомiжнi методи
    private static int[][] generateMatrix(int rows, int cols) {
        Random rand = new Random();
        int[][] matrix = new int[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                matrix[i][j] = rand.nextInt(10); // Числа 0-9
        return matrix;
    }

    private static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            for (int val : row) System.out.print(val + " ");
            System.out.println();
        }
    }

    // ==========================================
    // ЧАСТИНА 2: ПОШУК ФАЙЛiВ
    // ==========================================
    private static void runFileTask(Scanner scanner) {
        System.out.println("\n--- Пошук файлiв ---");
        System.out.print("Введiть шлях до директорiї (наприклад C:\\Projects): ");
        String path = scanner.nextLine();
        System.out.print("Введiть слово або лiтеру для пошуку в назвi: ");
        String keyword = scanner.nextLine();

        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("❌ Директорiя не знайдена або шлях некоректний.");
            return;
        }

        System.out.println("🔍 Починаю пошук...");
        long start = System.nanoTime();

        ForkJoinPool pool = new ForkJoinPool();
        FileSearchTask task = new FileSearchTask(dir, keyword);
        int count = pool.invoke(task);

        long end = System.nanoTime();

        System.out.println("✅ Знайдено файлiв: " + count);
        System.out.println("⏱️ Час пошуку: " + (end - start) / 1_000_000.0 + " мс");
    }

    // Рекурсивна задача для пошуку файлiв (Work Stealing пiдхiд)
    static class FileSearchTask extends RecursiveTask<Integer> {
        private File directory;
        private String keyword;

        public FileSearchTask(File directory, String keyword) {
            this.directory = directory;
            this.keyword = keyword;
        }

        @Override
        protected Integer compute() {
            int count = 0;
            List<FileSearchTask> subTasks = new ArrayList<>();

            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Створюємо пiдзадачу для нової папки
                        FileSearchTask task = new FileSearchTask(file, keyword);
                        task.fork(); // Вiдправляємо в чергу (Work Stealing pool може її забрати)
                        subTasks.add(task);
                    } else {
                        if (file.getName().contains(keyword)) {
                            count++;
                        }
                    }
                }
            }

            // Збираємо результати вiд усiх пiдзадач
            for (FileSearchTask task : subTasks) {
                count += task.join();
            }

            return count;
        }
    }
}