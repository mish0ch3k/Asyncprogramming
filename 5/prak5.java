import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public class prak5 {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println("--- Завдання 1: Отримання першого результату (anyOf) ---");
        performTask1();

        System.out.println("\n-----------------------------------\n");

        System.out.println("--- Завдання 2: Бронювання квитка (thenCombine, thenCompose) ---");
        performTask2();
    }

    private static void performTask1() throws ExecutionException, InterruptedException {
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
            simulateDelay(3000);
            return "Завдання 1 (довге)";
        });

        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
            simulateDelay(1000);
            return "Завдання 2 (швидке)";
        });

        CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> {
            simulateDelay(2000);
            return "Завдання 3 (середнє)";
        });

        CompletableFuture<Object> firstCompleted = CompletableFuture.anyOf(task1, task2, task3);

        System.out.println("Результат першого завершеного завдання: " + firstCompleted.get());
    }

    private static void performTask2() {
        CompletableFuture<Double> priceCheck = CompletableFuture.supplyAsync(() -> {
            simulateDelay(1000);
            System.out.println("-> Перевiрка цiни завершена");
            return 150.00; 
        });

        CompletableFuture<Boolean> availabilityCheck = CompletableFuture.supplyAsync(() -> {
            simulateDelay(1200);
            System.out.println("-> Перевiрка наявностi мiсць завершена");
            return true; 
        });

        priceCheck.thenCombine(availabilityCheck, (price, isAvailable) -> {
            if (isAvailable) {
                return "Квиток доступний. Найкраща цiна: " + price + "$";
            } else {
                throw new RuntimeException("Мiсць немає");
            }
        }).thenCompose(info -> {
            System.out.println(info);
            return processPayment(150.00);
        }).thenAccept(confirmation -> {
            System.out.println("Фiнальний статус: " + confirmation);
        }).join();
    }

    private static CompletableFuture<String> processPayment(double amount) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("-> Обробка платежу...");
            simulateDelay(1500);
            return "Оплата " + amount + "$ пройшла успiшно. Бронювання завершено.";
        });
    }

    private static void simulateDelay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}