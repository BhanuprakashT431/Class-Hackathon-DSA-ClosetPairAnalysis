import java.util.Locale;

/**
 * Automated test suite to verify all edge cases for Closest Pair of Points:
 * 1. N < 2 (Validation)
 * 2. N = 2 (Minimal dataset)
 * 3. Duplicate points (distance = 0.000000)
 * 4. Collinear Horizontal points (y = const)
 * 5. Collinear Vertical points (x = const, all share identical x)
 * 6. Collinear Diagonal points (y = m*x + c)
 * 7. Points sharing same x or y coordinates (Grid)
 * 8. Large random dataset (N = 5,000 points) ensuring BF and DC match exactly
 */
public class ClosestPairTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Closest Pair of Points - Automated Test Suite  ");
        System.out.println("=================================================");

        int passed = 0;
        int total = 0;

        // Test 1: N < 2
        total++;
        if (testNLessThanTwo()) {
            System.out.println("[PASS] Test 1: N < 2 Handling");
            passed++;
        } else {
            System.err.println("[FAIL] Test 1: N < 2 Handling");
        }

        // Test 2: N = 2
        total++;
        if (testN2()) {
            System.out.println("[PASS] Test 2: N = 2 Minimal Pair");
            passed++;
        } else {
            System.err.println("[FAIL] Test 2: N = 2 Minimal Pair");
        }

        // Test 3: Duplicate Points
        total++;
        if (testDuplicates()) {
            System.out.println("[PASS] Test 3: Duplicate Points (Distance = 0.000000)");
            passed++;
        } else {
            System.err.println("[FAIL] Test 3: Duplicate Points");
        }

        // Test 4: Collinear Horizontal (y = const)
        total++;
        if (testCollinearHorizontal()) {
            System.out.println("[PASS] Test 4: Collinear Horizontal Points (y = constant)");
            passed++;
        } else {
            System.err.println("[FAIL] Test 4: Collinear Horizontal Points");
        }

        // Test 5: Collinear Vertical (x = const, identical x)
        total++;
        if (testCollinearVertical()) {
            System.out.println("[PASS] Test 5: Collinear Vertical Points (x = constant)");
            passed++;
        } else {
            System.err.println("[FAIL] Test 5: Collinear Vertical Points");
        }

        // Test 6: Collinear Diagonal (y = x)
        total++;
        if (testCollinearDiagonal()) {
            System.out.println("[PASS] Test 6: Collinear Diagonal Points");
            passed++;
        } else {
            System.err.println("[FAIL] Test 6: Collinear Diagonal Points");
        }

        // Test 7: Shared X and Y Grid
        total++;
        if (testSharedCoordinatesGrid()) {
            System.out.println("[PASS] Test 7: Points Sharing Identical X and Y (Grid)");
            passed++;
        } else {
            System.err.println("[FAIL] Test 7: Grid Points");
        }

        // Test 8: Large Random Points Verification (N = 5,000)
        total++;
        if (testRandomLarge()) {
            System.out.println("[PASS] Test 8: Large Dataset (N = 5,000) BF vs D&C Match");
            passed++;
        } else {
            System.err.println("[FAIL] Test 8: Large Dataset Match");
        }

        System.out.println("=================================================");
        System.out.printf(Locale.US, "Summary: %d / %d Tests Passed (%.1f%%)%n",
                passed, total, (passed * 100.0 / total));
        System.out.println("=================================================");
    }

    private static boolean testNLessThanTwo() {
        ClosestPairAnalysis.Point[] pts0 = new ClosestPairAnalysis.Point[0];
        ClosestPairAnalysis.Point[] pts1 = new ClosestPairAnalysis.Point[]{
                new ClosestPairAnalysis.Point(10.0, 20.0, 0)
        };
        ClosestPairAnalysis.ClosestPairResult r0_bf = ClosestPairAnalysis.findClosestPairBruteForce(pts0);
        ClosestPairAnalysis.ClosestPairResult r0_dc = ClosestPairAnalysis.findClosestPairDivideAndConquer(pts0);
        ClosestPairAnalysis.ClosestPairResult r1_bf = ClosestPairAnalysis.findClosestPairBruteForce(pts1);
        ClosestPairAnalysis.ClosestPairResult r1_dc = ClosestPairAnalysis.findClosestPairDivideAndConquer(pts1);

        return r0_bf == null && r0_dc == null && r1_bf == null && r1_dc == null;
    }

    private static boolean testN2() {
        ClosestPairAnalysis.Point[] pts = new ClosestPairAnalysis.Point[]{
                new ClosestPairAnalysis.Point(0.0, 0.0, 0),
                new ClosestPairAnalysis.Point(3.0, 4.0, 1)
        };
        ClosestPairAnalysis.ClosestPairResult bf = ClosestPairAnalysis.findClosestPairBruteForce(pts);
        ClosestPairAnalysis.ClosestPairResult dc = ClosestPairAnalysis.findClosestPairDivideAndConquer(pts);

        return Math.abs(bf.distance - 5.0) < 1e-9 && Math.abs(dc.distance - 5.0) < 1e-9;
    }

    private static boolean testDuplicates() {
        ClosestPairAnalysis.Point[] pts = new ClosestPairAnalysis.Point[]{
                new ClosestPairAnalysis.Point(12.34, 56.78, 0),
                new ClosestPairAnalysis.Point(100.0, 200.0, 1),
                new ClosestPairAnalysis.Point(12.34, 56.78, 2), // exact duplicate of point 0
                new ClosestPairAnalysis.Point(300.0, 400.0, 3)
        };
        ClosestPairAnalysis.ClosestPairResult bf = ClosestPairAnalysis.findClosestPairBruteForce(pts);
        ClosestPairAnalysis.ClosestPairResult dc = ClosestPairAnalysis.findClosestPairDivideAndConquer(pts);

        return Math.abs(bf.distance - 0.0) < 1e-9 && Math.abs(dc.distance - 0.0) < 1e-9;
    }

    private static boolean testCollinearHorizontal() {
        // Horizontal line y = 10
        ClosestPairAnalysis.Point[] pts = new ClosestPairAnalysis.Point[]{
                new ClosestPairAnalysis.Point(5.0, 10.0, 0),
                new ClosestPairAnalysis.Point(20.0, 10.0, 1),
                new ClosestPairAnalysis.Point(22.5, 10.0, 2), // dist = 2.5
                new ClosestPairAnalysis.Point(50.0, 10.0, 3)
        };
        ClosestPairAnalysis.ClosestPairResult bf = ClosestPairAnalysis.findClosestPairBruteForce(pts);
        ClosestPairAnalysis.ClosestPairResult dc = ClosestPairAnalysis.findClosestPairDivideAndConquer(pts);

        return Math.abs(bf.distance - 2.5) < 1e-9 && Math.abs(dc.distance - 2.5) < 1e-9;
    }

    private static boolean testCollinearVertical() {
        // Vertical line x = 42: all points share the same x-coordinate!
        ClosestPairAnalysis.Point[] pts = new ClosestPairAnalysis.Point[]{
                new ClosestPairAnalysis.Point(42.0, 10.0, 0),
                new ClosestPairAnalysis.Point(42.0, 50.0, 1),
                new ClosestPairAnalysis.Point(42.0, 51.2, 2), // dist = 1.2
                new ClosestPairAnalysis.Point(42.0, 99.0, 3),
                new ClosestPairAnalysis.Point(42.0, 200.0, 4)
        };
        ClosestPairAnalysis.ClosestPairResult bf = ClosestPairAnalysis.findClosestPairBruteForce(pts);
        ClosestPairAnalysis.ClosestPairResult dc = ClosestPairAnalysis.findClosestPairDivideAndConquer(pts);

        return Math.abs(bf.distance - 1.2) < 1e-9 && Math.abs(dc.distance - 1.2) < 1e-9;
    }

    private static boolean testCollinearDiagonal() {
        ClosestPairAnalysis.Point[] pts = new ClosestPairAnalysis.Point[]{
                new ClosestPairAnalysis.Point(0.0, 0.0, 0),
                new ClosestPairAnalysis.Point(10.0, 10.0, 1),
                new ClosestPairAnalysis.Point(11.0, 11.0, 2), // dist = sqrt(2) = 1.41421356...
                new ClosestPairAnalysis.Point(50.0, 50.0, 3)
        };
        ClosestPairAnalysis.ClosestPairResult bf = ClosestPairAnalysis.findClosestPairBruteForce(pts);
        ClosestPairAnalysis.ClosestPairResult dc = ClosestPairAnalysis.findClosestPairDivideAndConquer(pts);
        double expected = Math.sqrt(2.0);

        return Math.abs(bf.distance - expected) < 1e-9 && Math.abs(dc.distance - expected) < 1e-9;
    }

    private static boolean testSharedCoordinatesGrid() {
        // 3x3 Grid: spacing 10
        ClosestPairAnalysis.Point[] pts = ClosestPairAnalysis.PointGenerator.generateGrid(9, 10.0);
        ClosestPairAnalysis.ClosestPairResult bf = ClosestPairAnalysis.findClosestPairBruteForce(pts);
        ClosestPairAnalysis.ClosestPairResult dc = ClosestPairAnalysis.findClosestPairDivideAndConquer(pts);

        return Math.abs(bf.distance - 10.0) < 1e-9 && Math.abs(dc.distance - 10.0) < 1e-9;
    }

    private static boolean testRandomLarge() {
        ClosestPairAnalysis.Point[] pts = ClosestPairAnalysis.PointGenerator.generateRandom(5000, false, 0.0, 10000.0);
        ClosestPairAnalysis.ClosestPairResult bf = ClosestPairAnalysis.findClosestPairBruteForce(pts);
        ClosestPairAnalysis.ClosestPairResult dc = ClosestPairAnalysis.findClosestPairDivideAndConquer(pts);

        boolean match = Math.abs(bf.distance - dc.distance) < 1e-9;
        System.out.printf(Locale.US, "       [Info] N = 5000: Min Distance = %.6f | BF Time: %.2f ms | D&C Time: %.2f ms%n",
                dc.distance, bf.getExecutionTimeMs(), dc.getExecutionTimeMs());
        return match;
    }
}
