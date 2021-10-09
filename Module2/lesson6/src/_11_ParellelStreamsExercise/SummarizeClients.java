package _11_ParellelStreamsExercise;

import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public final class SummarizeClients {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        List<UdacisearchClient> clients = ClientStore.getClients();
        int numClients = clients.size();

        // TODO: Create a ForkJoinPool to use as a thread pool.

        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());


        // TODO: For each metric below, turn it into a Future and submit it to the ForkJoinPool.

        Future<Integer> futureTotalQuarterlySpend = pool.submit(() -> clients
                .parallelStream()
                .mapToInt(UdacisearchClient::getQuarterlyBudget)
                .sum()
        );

        Future<Double> futureAverageBudget = pool.submit(() -> clients
                .parallelStream()
                .mapToDouble(UdacisearchClient::getQuarterlyBudget)
                .average()
                .orElse(0));

        Future<Long> futurenextExpiration = pool.submit(() -> clients
                .parallelStream()
                .min(Comparator.comparing(UdacisearchClient::getContractEnd))
                .map(UdacisearchClient::getId)
                .orElse(-1L));

        Future<List<ZoneId>> futureRepresentedZoneIds = pool.submit(() -> clients
                .parallelStream()
                .flatMap(c -> c.getTimeZones().stream())
                .distinct()  // Or use Collectors.toSet()
                .collect(Collectors.toList()));
        Future<Map<Year, Long>> futureContractPerYear = pool.submit(() -> clients
                .parallelStream()
                .collect(Collectors.groupingBy(SummarizeClients::getContractYear, Collectors.counting())));


        int totalQuarterlySpend = futureTotalQuarterlySpend.get();
        double averageBudget = futureAverageBudget.get();
        Long nextExpiration = futurenextExpiration.get();
        List<ZoneId> representedZoneIds = futureRepresentedZoneIds.get();
        Map<Year, Long> contractsPerYear = futureContractPerYear.get();
        System.out.println("Num clients: " + numClients);

        // TODO: You will need to call Future#get() on each of the futures to return the actual
        //       computed value.
        System.out.println("Total quarterly spend: " + totalQuarterlySpend);
        System.out.println("Average budget: " + averageBudget);
        System.out.println("ID of next expiring contract: " + nextExpiration);
        System.out.println("Client time zones: " + representedZoneIds);
        System.out.println("Contracts per year: " + contractsPerYear);
    }

    private static Year getContractYear(UdacisearchClient client) {
        LocalDate contractDate =
                LocalDate.ofInstant(client.getContractStart(), client.getTimeZones().get(0));
        return Year.of(contractDate.getYear());
    }
}
