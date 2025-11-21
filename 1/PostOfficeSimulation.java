import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;

// Клас, що представляє Пошту (спiльний ресурс)
class PostOffice {
    private final Queue<String> parcelQueue = new LinkedList<>();
    private final Queue<String> deliveryQueue = new LinkedList<>();
    private boolean isOpen = true;
    private final int MAX_QUEUE_SIZE = 5;

    // Метод для вiдправникiв (прийом посилок)
    public synchronized void sendParcel(String parcel, String senderName) {
        if (!isOpen) {
            System.out.println("❌ [Пошта]: Вибачте, " + senderName + ", пошта зачинена. Посилка не прийнята.");
            return;
        }

        while (parcelQueue.size() >= MAX_QUEUE_SIZE) {
            try {
                System.out.println("⏳ [Пошта]: Черга переповнена. " + senderName + " чекає...");
                wait(); // Чекаємо, поки працiвник звiльнить мiсце
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (isOpen) {
            parcelQueue.add(parcel);
            System.out.println("📥 [Прийом]: " + senderName + " вiдправив: " + parcel);
            notifyAll(); // Повiдомляємо працiвника, що є робота
        }
    }

    // Метод для працiвника пошти (обробка посилок)
    public synchronized void processParcel() {
        // Працiвник працює поки пошта вiдкрита АБО поки є необробленi посилки
        while (parcelQueue.isEmpty()) {
            if (!isOpen) {
                return; // Якщо пошта закрита i черга пуста - йдемо додому
            }
            try {
                wait(); // Чекаємо на новi посилки
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        String parcel = parcelQueue.poll();
        System.out.println("⚙️ [Працiвник]: Обробляє " + parcel + "...");
        notifyAll(); // Повiдомляємо вiдправникiв, що мiсце звiльнилось

        // iмiтацiя роботи
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Передаємо на доставку
        addToDelivery(parcel);
    }

    private synchronized void addToDelivery(String parcel) {
        deliveryQueue.add(parcel);
        System.out.println("🚚 [Логiстика]: " + parcel + " передано кур'єрам.");
        notifyAll(); // Повiдомляємо отримувачiв
    }

    // Метод для отримувачiв
    public synchronized void receiveParcel(String receiverName) {
        while (deliveryQueue.isEmpty()) {
            if (!isOpen && parcelQueue.isEmpty()) {
                return; // Якщо все закрито i пусто
            }
            try {
                wait(1000); // Чекаємо трохи i перевiряємо знову
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        String parcel = deliveryQueue.poll();
        if (parcel != null) {
            System.out.println("✅ [Отримання]: " + receiverName + " отримав: " + parcel);
        }
    }

    // Закриття пошти
    public synchronized void closePostOffice() {
        isOpen = false;
        System.out.println("\n🔴 =============== ПОШТА ЗАЧИНЯЄТЬСЯ =============== 🔴");
        System.out.println("📢 Новi посилки не приймаються, але старi будуть доставленi.\n");
        notifyAll(); // Будимо всi потоки, щоб вони могли коректно завершити роботу
    }
    
    public boolean isWorkFinished() {
        return !isOpen && parcelQueue.isEmpty() && deliveryQueue.isEmpty();
    }
}

// Потiк вiдправника
class Sender implements Runnable {
    private final PostOffice postOffice;
    private final String name;

    public Sender(PostOffice postOffice, String name) {
        this.postOffice = postOffice;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i <= 3; i++) {
                postOffice.sendParcel("Посилка №" + i + " вiд " + name, name);
                Thread.sleep(new Random().nextInt(1000) + 500); // Випадкова затримка мiж вiзитами
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

// Потiк працiвника
class PostWorker implements Runnable {
    private final PostOffice postOffice;

    public PostWorker(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @Override
    public void run() {
        while (!postOffice.isWorkFinished()) {
            postOffice.processParcel();
        }
        System.out.println("🏁 [Працiвник]: Всi посилки оброблено. Змiна закiнчена.");
    }
}

// Потiк отримувача
class Receiver implements Runnable {
    private final PostOffice postOffice;
    private final String name;

    public Receiver(PostOffice postOffice, String name) {
        this.postOffice = postOffice;
        this.name = name;
    }

    @Override
    public void run() {
        while (!postOffice.isWorkFinished()) {
            postOffice.receiveParcel(name);
        }
    }
}

public class PostOfficeSimulation {
    public static void main(String[] args) {
        PostOffice postOffice = new PostOffice();

        System.out.println("🟢 =============== ПОШТА ВiДКРИТА =============== 🟢");

        // Створення єдиного працiвника
        Thread worker = new Thread(new PostWorker(postOffice));
        worker.start();

        // Створення вiдправникiв (3 особи)
        for (int i = 1; i <= 3; i++) {
            new Thread(new Sender(postOffice, "Вiдправник-" + i)).start();
        }

        // Створення отримувачiв (3 особи)
        for (int i = 1; i <= 3; i++) {
            new Thread(new Receiver(postOffice, "Отримувач-" + i)).start();
        }

        // Симуляцiя робочого дня (наприклад, 5 секунд)
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Закриття пошти
        postOffice.closePostOffice();
    }
}