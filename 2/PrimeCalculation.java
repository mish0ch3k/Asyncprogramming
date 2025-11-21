import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class PrimeCalculation {
    private static final List<Integer> primeNumbers = new CopyOnWriteArrayList<>();
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введiть число N (кiнець дiапазону, наприклад, 1000): ");
        int n = scanner.nextInt();

        int numberOfThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        
        List<Future<List<Integer>>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        int chunkSize = n / numberOfThreads;
        
        for (int i = 0; i < numberOfThreads; i++) {
            int start = i * chunkSize;
            int end = (i == numberOfThreads - 1) ? n : (start + chunkSize - 1);

            Callable<List<Integer>> task = new PrimeSearcher(start, end);
            
            Future<List<Integer>> future = executor.submit(task);
            futures.add(future);
        }

        for (Future<List<Integer>> future : futures) {
            try {
                while (!future.isDone()) {
                   Thread.sleep(10); 
                }

                if (!future.isCancelled()) {
                    List<Integer> result = future.get();
                    primeNumbers.addAll(result);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();

        long endTime = System.currentTimeMillis();

        System.out.println("\n✅ Знайдено простих чисел: " + primeNumbers.size());
        System.out.println("📋 Список (першi 20 для прикладу): " + 
                           (primeNumbers.size() > 20 ? primeNumbers.subList(0, 20) + "..." : primeNumbers));
        System.out.println("⏱️ Час виконання програми: " + (endTime - startTime) + " мс");
    }
}

class PrimeSearcher implements Callable<List<Integer>> {
    private final int start;
    private final int end;

    public PrimeSearcher(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public List<Integer> call() throws Exception {
        List<Integer> foundPrimes = new ArrayList<>();
        String threadName = Thread.currentThread().getName();
        System.out.println("🧵 [" + threadName + "] Обробляє дiапазон: " + start + " - " + end);

        for (int i = start; i <= end; i++) {
            if (isPrime(i)) {
                foundPrimes.add(i);
            }
        }
        return foundPrimes;
    }

    private boolean isPrime(int num) {
        if (num <= 1) return false;
        for (int i = 2; i <= Math.sqrt(num); i++) {
            if (num % i == 0) return false;
        }
        return true;
    }
}