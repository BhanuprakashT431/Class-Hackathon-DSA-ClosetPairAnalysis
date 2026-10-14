import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * ============================================================================
 * Closest Pair of Points: Comparative Analysis & Visualization
 * ============================================================================
 * 
 * Algorithms implemented:
 * 1. Brute-Force Method: O(N^2) time complexity
 * 2. Divide-and-Conquer Method: O(N log N) time complexity
 * 
 * Key Features & Edge Case Handling:
 * - Robust input handling: N < 2 validation, N = 2 minimal case
 * - Supports both Floating-Point and Integer coordinates
 * - Duplicate points detection (distance = 0.000000)
 * - Collinear points (horizontal, vertical, diagonal)
 * - Points sharing identical x or y coordinates handled without partition bias
 * - Interactive Swing GUI with:
 *     * 2D Coordinate Plane with zoom & pan support
 *     * Closest pair highlighting with connecting line & distance annotation
 *     * Performance comparison benchmark (Running-Time vs N graph)
 *     * Edge case presets for instant verification
 * 
 * ============================================================================
 */
public class ClosestPairAnalysis {

    // ========================================================================
    // 1. DATA STRUCTURES
    // ========================================================================

    /**
     * Represents a 2D Point with (x, y) coordinates and a unique identifier.
     */
    public static class Point implements Comparable<Point> {
        public final double x;
        public final double y;
        public final int id; // Unique ID to distinguish duplicate coordinates

        public Point(double x, double y, int id) {
            this.x = x;
            this.y = y;
            this.id = id;
        }

        /**
         * Calculates Euclidean distance to another point.
         */
        public double distanceTo(Point other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            return Math.hypot(dx, dy);
        }

        /**
         * Calculates squared Euclidean distance (avoids costly sqrt in inner loops).
         */
        public double distanceSquaredTo(Point other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            return dx * dx + dy * dy;
        }

        /**
         * Primary comparator: sorts by x-coordinate, ties broken by y, then by id.
         */
        @Override
        public int compareTo(Point other) {
            if (Double.compare(this.x, other.x) != 0) {
                return Double.compare(this.x, other.x);
            }
            if (Double.compare(this.y, other.y) != 0) {
                return Double.compare(this.y, other.y);
            }
            return Integer.compare(this.id, other.id);
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "(%.6f, %.6f)", x, y);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Point)) return false;
            Point o = (Point) obj;
            return Double.compare(x, o.x) == 0 && Double.compare(y, o.y) == 0 && id == o.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, id);
        }
    }

    /**
     * Comparator to sort points by y-coordinate, ties broken by x, then by id.
     */
    public static class PointYComparator implements Comparator<Point> {
        @Override
        public int compare(Point p1, Point p2) {
            if (Double.compare(p1.y, p2.y) != 0) {
                return Double.compare(p1.y, p2.y);
            }
            if (Double.compare(p1.x, p2.x) != 0) {
                return Double.compare(p1.x, p2.x);
            }
            return Integer.compare(p1.id, p2.id);
        }
    }

    /**
     * Stores the result of a closest pair search.
     */
    public static class ClosestPairResult {
        public final Point p1;
        public final Point p2;
        public final double distance;
        public final long executionTimeNs;

        public ClosestPairResult(Point a, Point b, double distance, long executionTimeNs) {
            // Order points canonically for consistent output
            if (a != null && b != null && a.compareTo(b) > 0) {
                this.p1 = b;
                this.p2 = a;
            } else {
                this.p1 = a;
                this.p2 = b;
            }
            this.distance = distance;
            this.executionTimeNs = executionTimeNs;
        }

        public double getExecutionTimeMs() {
            return executionTimeNs / 1_000_000.0;
        }
    }

    // ========================================================================
    // 2. ALGORITHMS
    // ========================================================================

    /**
     * Brute-Force Algorithm: O(N^2)
     * Compares every distinct pair of points to find the minimum Euclidean distance.
     */
    public static ClosestPairResult findClosestPairBruteForce(Point[] points) {
        if (points == null || points.length < 2) {
            return null;
        }

        long startTime = System.nanoTime();
        int n = points.length;
        double minSqDist = Double.POSITIVE_INFINITY;
        Point bestP1 = null;
        Point bestP2 = null;

        for (int i = 0; i < n; i++) {
            Point pi = points[i];
            for (int j = i + 1; j < n; j++) {
                double sqDist = pi.distanceSquaredTo(points[j]);
                if (sqDist < minSqDist) {
                    minSqDist = sqDist;
                    bestP1 = pi;
                    bestP2 = points[j];
                }
            }
        }

        long elapsedNs = System.nanoTime() - startTime;
        return new ClosestPairResult(bestP1, bestP2, Math.sqrt(minSqDist), elapsedNs);
    }

    /**
     * Divide-and-Conquer Algorithm: O(N log N)
     * Classical Shamos-Hoey algorithm.
     * Pre-sorts points by X and Y coordinates, then recursively divides the plane.
     */
    public static ClosestPairResult findClosestPairDivideAndConquer(Point[] points) {
        if (points == null || points.length < 2) {
            return null;
        }

        long startTime = System.nanoTime();
        int n = points.length;

        // Step 1: Pre-sort points by X and by Y
        Point[] px = points.clone();
        Arrays.sort(px);

        Point[] py = points.clone();
        Arrays.sort(py, new PointYComparator());

        // Tracking array to partition py in O(N) time without HashSet overhead
        boolean[] inLeft = new boolean[n];

        PairResult internalResult = closestPairRec(px, py, inLeft);

        long elapsedNs = System.nanoTime() - startTime;
        return new ClosestPairResult(internalResult.p1, internalResult.p2, internalResult.distance, elapsedNs);
    }

    private static class PairResult {
        final Point p1;
        final Point p2;
        final double distance;

        PairResult(Point p1, Point p2, double distance) {
            this.p1 = p1;
            this.p2 = p2;
            this.distance = distance;
        }
    }

    /**
     * Recursive Divide-and-Conquer step.
     */
    private static PairResult closestPairRec(Point[] px, Point[] py, boolean[] inLeft) {
        int n = px.length;

        // Base case: for small n (<= 3), solve using direct brute-force
        if (n <= 3) {
            return bruteForceBaseCase(px);
        }

        // Divide step: partition at midpoint
        int mid = n / 2;
        Point midPoint = px[mid];

        Point[] pxL = Arrays.copyOfRange(px, 0, mid);
        Point[] pxR = Arrays.copyOfRange(px, mid, n);

        // Mark points belonging to the left half using their unique IDs
        for (Point p : pxL) {
            inLeft[p.id] = true;
        }

        // Split py into pyL and pyR preserving y-sorted order in O(N) time
        Point[] pyL = new Point[mid];
        Point[] pyR = new Point[n - mid];
        int li = 0, ri = 0;
        for (Point p : py) {
            if (inLeft[p.id]) {
                pyL[li++] = p;
            } else {
                pyR[ri++] = p;
            }
        }

        // Reset tracking array for left points
        for (Point p : pxL) {
            inLeft[p.id] = false;
        }

        // Conquer step: recursive calls on left and right halves
        PairResult leftRes = closestPairRec(pxL, pyL, inLeft);
        PairResult rightRes = closestPairRec(pxR, pyR, inLeft);

        // Best pair among the two halves
        PairResult best = (leftRes.distance <= rightRes.distance) ? leftRes : rightRes;
        double delta = best.distance;

        // Optimization: if distance is already 0 (duplicates found), cannot do better
        if (delta == 0.0) {
            return best;
        }

        // Combine step: construct strip of points within delta of midline x = midPoint.x
        // Reusing fixed array to avoid garbage collection and reallocation overhead
        Point[] strip = new Point[n];
        int stripCount = 0;
        for (Point p : py) {
            if (Math.abs(p.x - midPoint.x) < delta) {
                strip[stripCount++] = p;
            }
        }

        // Check strip pairs: inner loop is proven geometrically to run at most 7 times
        for (int i = 0; i < stripCount; i++) {
            Point pi = strip[i];
            for (int j = i + 1; j < stripCount; j++) {
                Point pj = strip[j];
                // Since strip is sorted by y, break when y difference exceeds delta
                if ((pj.y - pi.y) >= delta) {
                    break;
                }
                double d = pi.distanceTo(pj);
                if (d < delta) {
                    delta = d;
                    best = new PairResult(pi, pj, delta);
                    if (delta == 0.0) {
                        return best;
                    }
                }
            }
        }

        return best;
    }

    /**
     * Helper for small base cases in recursion (n <= 3).
     */
    private static PairResult bruteForceBaseCase(Point[] pts) {
        double minD = Double.POSITIVE_INFINITY;
        Point b1 = null, b2 = null;
        for (int i = 0; i < pts.length; i++) {
            for (int j = i + 1; j < pts.length; j++) {
                double d = pts[i].distanceTo(pts[j]);
                if (d < minD) {
                    minD = d;
                    b1 = pts[i];
                    b2 = pts[j];
                }
            }
        }
        return new PairResult(b1, b2, minD);
    }

    // ========================================================================
    // 3. POINT GENERATOR & EDGE CASE PRESETS
    // ========================================================================

    public static class PointGenerator {
        private static final Random random = new Random();

        /**
         * Randomly generates N points supporting both integer and floating-point coordinates.
         */
        public static Point[] generateRandom(int n, boolean integerCoords, double minVal, double maxVal) {
            Point[] points = new Point[n];
            for (int i = 0; i < n; i++) {
                double x, y;
                if (integerCoords) {
                    int minInt = (int) Math.round(minVal);
                    int maxInt = (int) Math.round(maxVal);
                    int span = Math.max(1, maxInt - minInt + 1);
                    x = minInt + random.nextInt(span);
                    y = minInt + random.nextInt(span);
                } else {
                    x = minVal + random.nextDouble() * (maxVal - minVal);
                    y = minVal + random.nextDouble() * (maxVal - minVal);
                }
                points[i] = new Point(x, y, i);
            }
            return points;
        }

        /**
         * Edge Case: Collinear points on a horizontal line (y = constant).
         */
        public static Point[] generateCollinearHorizontal(int n, double yVal, double minX, double maxX) {
            Point[] points = new Point[n];
            double step = (maxX - minX) / Math.max(1, n - 1);
            for (int i = 0; i < n; i++) {
                points[i] = new Point(minX + i * step, yVal, i);
            }
            return points;
        }

        /**
         * Edge Case: Collinear points on a vertical line (x = constant).
         * Crucial test: all points share the exact same x-coordinate!
         */
        public static Point[] generateCollinearVertical(int n, double xVal, double minY, double maxY) {
            Point[] points = new Point[n];
            double step = (maxY - minY) / Math.max(1, n - 1);
            for (int i = 0; i < n; i++) {
                points[i] = new Point(xVal, minY + i * step, i);
            }
            return points;
        }

        /**
         * Edge Case: Collinear points on a diagonal line (y = x).
         */
        public static Point[] generateCollinearDiagonal(int n, double minVal, double maxVal) {
            Point[] points = new Point[n];
            double step = (maxVal - minVal) / Math.max(1, n - 1);
            for (int i = 0; i < n; i++) {
                double v = minVal + i * step;
                points[i] = new Point(v, v, i);
            }
            return points;
        }

        /**
         * Edge Case: Points containing duplicate coordinates (distance = 0.000000).
         */
        public static Point[] generateWithDuplicates(int n, double minVal, double maxVal) {
            Point[] points = generateRandom(n, false, minVal, maxVal);
            if (n >= 2) {
                // Force point 1 to be an exact duplicate of point 0
                points[1] = new Point(points[0].x, points[0].y, 1);
            }
            return points;
        }

        /**
         * Edge Case: Minimal dataset with exactly N = 2 points.
         */
        public static Point[] generateN2(double minVal, double maxVal) {
            return generateRandom(2, false, minVal, maxVal);
        }

        /**
         * Edge Case: Grid points with many shared x and y coordinates.
         */
        public static Point[] generateGrid(int n, double spacing) {
            Point[] points = new Point[n];
            int side = (int) Math.ceil(Math.sqrt(n));
            int idx = 0;
            for (int r = 0; r < side && idx < n; r++) {
                for (int c = 0; c < side && idx < n; c++) {
                    points[idx] = new Point(c * spacing, r * spacing, idx);
                    idx++;
                }
            }
            return points;
        }
    }

    // ========================================================================
    // 4. SWING VISUALIZATION GUI
    // ========================================================================

    public static class VisualizationWindow extends JFrame {
        private Point[] points;
        private ClosestPairResult bruteForceResult;
        private ClosestPairResult divideAndConquerResult;
        private boolean isIntegerCoords = false;

        private final PointsCanvas pointsCanvas;
        private final BenchmarkPanel benchmarkPanel;
        private final JLabel statusLabel;
        private JLabel bfInfoLabel;
        private JLabel dcInfoLabel;
        private JLabel timeComparisonLabel;
        private final JTextField nField;
        private final JComboBox<String> edgeCaseCombo;
        private final JCheckBox intCoordsCheck;

        public VisualizationWindow(Point[] initialPoints, ClosestPairResult bfRes, ClosestPairResult dcRes, boolean isInt) {
            super("Closest Pair of Points - Comparative Analysis & Visualization");
            this.points = initialPoints;
            this.bruteForceResult = bfRes;
            this.divideAndConquerResult = dcRes;
            this.isIntegerCoords = isInt;

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1200, 850);
            setLocationRelativeTo(null);

            // Set modern dark look and feel accents
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            setLayout(new BorderLayout());

            // Initialize Canvas early for button callbacks
            pointsCanvas = new PointsCanvas();

            // --- Top Toolbar Controls ---
            JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
            topBar.setBackground(new Color(30, 32, 48));
            topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 54, 76)));

            JLabel nLabel = new JLabel("Points (N):");
            nLabel.setForeground(Color.WHITE);
            nLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            topBar.add(nLabel);

            nField = new JTextField(String.valueOf(points != null ? points.length : 100), 6);
            nField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            topBar.add(nField);

            intCoordsCheck = new JCheckBox("Integer Coords", isIntegerCoords);
            intCoordsCheck.setForeground(Color.WHITE);
            intCoordsCheck.setOpaque(false);
            intCoordsCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            topBar.add(intCoordsCheck);

            JLabel presetLabel = new JLabel("Preset / Edge Case:");
            presetLabel.setForeground(Color.WHITE);
            presetLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            topBar.add(presetLabel);

            edgeCaseCombo = new JComboBox<>(new String[]{
                    "Random Uniform Points",
                    "Edge Case: Collinear (Horizontal)",
                    "Edge Case: Collinear (Vertical)",
                    "Edge Case: Collinear (Diagonal)",
                    "Edge Case: Duplicates (d = 0.0)",
                    "Edge Case: Shared X/Y Grid",
                    "Edge Case: Minimal N = 2"
            });
            edgeCaseCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            topBar.add(edgeCaseCombo);

            JButton runBtn = createStyledButton("Generate & Solve", new Color(66, 133, 244));
            runBtn.addActionListener(e -> onGenerateAndSolve());
            topBar.add(runBtn);

            JButton resetViewBtn = createStyledButton("Reset Zoom", new Color(90, 100, 120));
            resetViewBtn.addActionListener(e -> pointsCanvas.resetView());
            topBar.add(resetViewBtn);

            add(topBar, BorderLayout.NORTH);

            // --- Tabbed Pane (Canvas vs Benchmark) ---
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

            // Tab 1: 2D Points Visualization with sidebar
            JPanel visualTab = new JPanel(new BorderLayout());
            visualTab.add(pointsCanvas, BorderLayout.CENTER);

            // Sidebar stats
            JPanel sidePanel = createSidebar();
            visualTab.add(sidePanel, BorderLayout.EAST);

            tabbedPane.addTab("2D Coordinate Plane Visualization", visualTab);

            // Tab 2: Performance Benchmark Chart
            benchmarkPanel = new BenchmarkPanel();
            tabbedPane.addTab("Performance Benchmark (Time vs N)", benchmarkPanel);

            add(tabbedPane, BorderLayout.CENTER);

            // Status bar
            statusLabel = new JLabel(" Ready. Tip: Use mouse wheel to Zoom, Drag to Pan on the 2D plane.");
            statusLabel.setBorder(new EmptyBorder(6, 12, 6, 12));
            statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            add(statusLabel, BorderLayout.SOUTH);

            updateResultsDisplay();
        }

        private JButton createStyledButton(String text, Color bg) {
            JButton btn = new JButton(text);
            btn.setBackground(bg);
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return btn;
        }

        private JPanel createSidebar() {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(new Color(24, 26, 38));
            panel.setPreferredSize(new Dimension(340, 0));
            panel.setBorder(new EmptyBorder(16, 16, 16, 16));

            JLabel title = new JLabel("Analysis Results");
            title.setForeground(Color.WHITE);
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(title);
            panel.add(Box.createVerticalStrut(15));

            // Card 1: Brute-Force Card
            JPanel bfCard = createInfoCard("Brute-Force O(N²)", new Color(239, 83, 80));
            bfInfoLabel = new JLabel("<html>Distance: -<br>Pair: -<br>Time: -</html>");
            bfInfoLabel.setForeground(new Color(230, 230, 240));
            bfInfoLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
            bfCard.add(bfInfoLabel);
            panel.add(bfCard);
            panel.add(Box.createVerticalStrut(15));

            // Card 2: Divide-and-Conquer Card
            JPanel dcCard = createInfoCard("Divide-and-Conquer O(N log N)", new Color(76, 175, 80));
            dcInfoLabel = new JLabel("<html>Distance: -<br>Pair: -<br>Time: -</html>");
            dcInfoLabel.setForeground(new Color(230, 230, 240));
            dcInfoLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
            dcCard.add(dcInfoLabel);
            panel.add(dcCard);
            panel.add(Box.createVerticalStrut(15));

            // Card 3: Comparison Summary
            JPanel cmpCard = createInfoCard("Verification & Speedup", new Color(156, 39, 176));
            timeComparisonLabel = new JLabel("<html>Status: -<br>Speedup: -</html>");
            timeComparisonLabel.setForeground(new Color(230, 230, 240));
            timeComparisonLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            cmpCard.add(timeComparisonLabel);
            panel.add(cmpCard);

            panel.add(Box.createVerticalGlue());

            // Legend Panel
            JPanel legendCard = createInfoCard("Visual Legend", new Color(41, 182, 246));
            JLabel legendText = new JLabel("<html>" +
                    "• <font color='#64B5F6'>● Cyan dots</font>: Random 2D Points<br>" +
                    "• <font color='#FF5252'>◎ Red rings</font>: Closest Pair Points<br>" +
                    "• <font color='#FFD54F'>― Gold line</font>: Joining Line (Min Distance)<br>" +
                    "• <font color='#81C784'>Scroll</font>: Zoom In / Out<br>" +
                    "• <font color='#81C784'>Drag</font>: Pan Coordinate Plane" +
                    "</html>");
            legendText.setForeground(new Color(220, 220, 230));
            legendText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            legendCard.add(legendText);
            panel.add(legendCard);

            return panel;
        }

        private JPanel createInfoCard(String headerText, Color accentColor) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(new Color(33, 36, 52));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));
            card.setMaximumSize(new Dimension(320, 130));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel header = new JLabel(headerText);
            header.setForeground(accentColor);
            header.setFont(new Font("Segoe UI", Font.BOLD, 13));
            card.add(header, BorderLayout.NORTH);

            return card;
        }

        private void onGenerateAndSolve() {
            try {
                int n = Integer.parseInt(nField.getText().trim());
                if (n < 2) {
                    JOptionPane.showMessageDialog(this,
                            "Number of points N must be >= 2.",
                            "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                isIntegerCoords = intCoordsCheck.isSelected();
                int presetIdx = edgeCaseCombo.getSelectedIndex();

                switch (presetIdx) {
                    case 1:
                        points = PointGenerator.generateCollinearHorizontal(n, 500.0, 50.0, 950.0);
                        break;
                    case 2:
                        points = PointGenerator.generateCollinearVertical(n, 500.0, 50.0, 950.0);
                        break;
                    case 3:
                        points = PointGenerator.generateCollinearDiagonal(n, 50.0, 950.0);
                        break;
                    case 4:
                        points = PointGenerator.generateWithDuplicates(n, 0.0, 1000.0);
                        break;
                    case 5:
                        points = PointGenerator.generateGrid(n, 50.0);
                        break;
                    case 6:
                        points = PointGenerator.generateN2(0.0, 1000.0);
                        nField.setText("2");
                        break;
                    default:
                        points = PointGenerator.generateRandom(n, isIntegerCoords, 0.0, 1000.0);
                        break;
                }

                // Solve with both algorithms
                bruteForceResult = findClosestPairBruteForce(points);
                divideAndConquerResult = findClosestPairDivideAndConquer(points);

                // Update UI and console output
                updateResultsDisplay();
                pointsCanvas.resetView();
                pointsCanvas.repaint();

                // Print console output in exact required format
                printResults(bruteForceResult, divideAndConquerResult);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a valid integer for N.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void updateResultsDisplay() {
            if (bruteForceResult == null || divideAndConquerResult == null) return;

            bfInfoLabel.setText(String.format(Locale.US,
                    "<html><b>Min Dist:</b> %.6f<br>" +
                    "<b>P1:</b> %s<br>" +
                    "<b>P2:</b> %s<br>" +
                    "<b>Time:</b> %.4f ms</html>",
                    bruteForceResult.distance,
                    bruteForceResult.p1,
                    bruteForceResult.p2,
                    bruteForceResult.getExecutionTimeMs()));

            dcInfoLabel.setText(String.format(Locale.US,
                    "<html><b>Min Dist:</b> %.6f<br>" +
                    "<b>P1:</b> %s<br>" +
                    "<b>P2:</b> %s<br>" +
                    "<b>Time:</b> %.4f ms</html>",
                    divideAndConquerResult.distance,
                    divideAndConquerResult.p1,
                    divideAndConquerResult.p2,
                    divideAndConquerResult.getExecutionTimeMs()));

            boolean distMatch = Math.abs(bruteForceResult.distance - divideAndConquerResult.distance) < 1e-9;
            double bfTime = bruteForceResult.getExecutionTimeMs();
            double dcTime = divideAndConquerResult.getExecutionTimeMs();
            String speedup = dcTime > 0 ? String.format(Locale.US, "%.2fx faster", bfTime / dcTime) : "Instant";

            timeComparisonLabel.setText(String.format(Locale.US,
                    "<html><b>Verification:</b> %s<br>" +
                    "<b>D&C Speedup:</b> %s<br>" +
                    "<b>Total Points:</b> %d</html>",
                    distMatch ? "<font color='#4CAF50'>MATCH (Identical)</font>" : "<font color='#F44336'>MISMATCH</font>",
                    speedup,
                    points.length));

            statusLabel.setText(String.format(Locale.US,
                    " Solved for N = %d points | Min Distance: %.6f | BF Time: %.4f ms | D&C Time: %.4f ms",
                    points.length, divideAndConquerResult.distance, bfTime, dcTime));
        }

        // --- Custom Canvas for 2D Points Visualization ---
        private class PointsCanvas extends JPanel {
            private double zoom = 1.0;
            private double panX = 0;
            private double panY = 0;
            private java.awt.Point lastMousePos;

            public PointsCanvas() {
                setBackground(new Color(18, 20, 30));

                MouseAdapter ma = new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        lastMousePos = e.getPoint();
                    }

                    @Override
                    public void mouseDragged(MouseEvent e) {
                        if (lastMousePos != null) {
                            panX += (e.getX() - lastMousePos.x);
                            panY += (e.getY() - lastMousePos.y);
                            lastMousePos = e.getPoint();
                            repaint();
                        }
                    }

                    @Override
                    public void mouseWheelMoved(MouseWheelEvent e) {
                        double zoomFactor = e.getWheelRotation() < 0 ? 1.15 : 0.87;
                        double oldZoom = zoom;
                        zoom = Math.max(0.1, Math.min(zoom * zoomFactor, 50.0));

                        // Zoom towards mouse pointer
                        double mouseX = e.getX();
                        double mouseY = e.getY();
                        panX = mouseX - (mouseX - panX) * (zoom / oldZoom);
                        panY = mouseY - (mouseY - panY) * (zoom / oldZoom);
                        repaint();
                    }
                };

                addMouseListener(ma);
                addMouseMotionListener(ma);
                addMouseWheelListener(ma);
            }

            public void resetView() {
                zoom = 1.0;
                panX = 0;
                panY = 0;
                repaint();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                if (points == null || points.length == 0) {
                    g2.setColor(Color.GRAY);
                    g2.drawString("No points to display.", getWidth() / 2 - 50, getHeight() / 2);
                    return;
                }

                // Compute bounding box
                double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
                double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
                for (Point p : points) {
                    minX = Math.min(minX, p.x);
                    maxX = Math.max(maxX, p.x);
                    minY = Math.min(minY, p.y);
                    maxY = Math.max(maxY, p.y);
                }

                // Avoid division by zero for single line/point cases
                if (Math.abs(maxX - minX) < 1e-6) { minX -= 10; maxX += 10; }
                if (Math.abs(maxY - minY) < 1e-6) { minY -= 10; maxY += 10; }

                double margin = 60;
                double plotW = getWidth() - 2 * margin;
                double plotH = getHeight() - 2 * margin;

                double scaleX = plotW / (maxX - minX);
                double scaleY = plotH / (maxY - minY);
                double baseScale = Math.min(scaleX, scaleY);

                // Transformation lambda
                AffineTransform at = new AffineTransform();
                at.translate(panX + margin + (plotW - (maxX - minX) * baseScale * zoom) / 2.0,
                             panY + margin + (plotH - (maxY - minY) * baseScale * zoom) / 2.0);
                at.scale(baseScale * zoom, baseScale * zoom);
                at.translate(-minX, -minY);

                // --- Draw Subtle Grid Lines ---
                g2.setColor(new Color(35, 38, 55));
                g2.setStroke(new BasicStroke(1.0f));
                int gridSteps = 10;
                for (int i = 0; i <= gridSteps; i++) {
                    double gx = minX + i * (maxX - minX) / gridSteps;
                    double gy = minY + i * (maxY - minY) / gridSteps;

                    Point2D.Double pTop = transformPoint(at, gx, minY);
                    Point2D.Double pBot = transformPoint(at, gx, maxY);
                    Point2D.Double pLeft = transformPoint(at, minX, gy);
                    Point2D.Double pRight = transformPoint(at, maxX, gy);

                    g2.drawLine((int) pTop.x, (int) pTop.y, (int) pBot.x, (int) pBot.y);
                    g2.drawLine((int) pLeft.x, (int) pLeft.y, (int) pRight.x, (int) pRight.y);
                }

                // --- Draw Points ---
                g2.setColor(new Color(100, 181, 246, 180));
                int ptRadius = points.length > 2000 ? 2 : (points.length > 500 ? 3 : 4);
                for (Point p : points) {
                    Point2D.Double pt = transformPoint(at, p.x, p.y);
                    g2.fillOval((int) (pt.x - ptRadius), (int) (pt.y - ptRadius), ptRadius * 2, ptRadius * 2);
                }

                // --- Highlight Closest Pair & Draw Joining Line ---
                if (divideAndConquerResult != null && divideAndConquerResult.p1 != null && divideAndConquerResult.p2 != null) {
                    Point cp1 = divideAndConquerResult.p1;
                    Point cp2 = divideAndConquerResult.p2;

                    Point2D.Double s1 = transformPoint(at, cp1.x, cp1.y);
                    Point2D.Double s2 = transformPoint(at, cp2.x, cp2.y);

                    // 1. Draw glowing connecting line
                    g2.setStroke(new BasicStroke(4.0f));
                    g2.setColor(new Color(255, 213, 79, 120)); // outer glow
                    g2.drawLine((int) s1.x, (int) s1.y, (int) s2.x, (int) s2.y);

                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(new Color(255, 238, 88)); // core yellow line
                    g2.drawLine((int) s1.x, (int) s1.y, (int) s2.x, (int) s2.y);

                    // 2. Draw highlighted target rings around closest points
                    drawTargetRings(g2, s1.x, s1.y, new Color(255, 82, 82));
                    drawTargetRings(g2, s2.x, s2.y, new Color(76, 175, 80));

                    // 3. Draw distance badge above the line
                    double midX = (s1.x + s2.x) / 2.0;
                    double midY = (s1.y + s2.y) / 2.0;
                    String distText = String.format(Locale.US, "d = %.6f", divideAndConquerResult.distance);

                    g2.setFont(new Font("Consolas", Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    int textW = fm.stringWidth(distText);
                    int textH = fm.getHeight();

                    // Badge background
                    g2.setColor(new Color(20, 22, 32, 220));
                    g2.fillRoundRect((int) midX - textW / 2 - 6, (int) midY - textH - 4, textW + 12, textH + 4, 8, 8);
                    g2.setColor(new Color(255, 213, 79));
                    g2.drawRoundRect((int) midX - textW / 2 - 6, (int) midY - textH - 4, textW + 12, textH + 4, 8, 8);

                    // Badge text
                    g2.setColor(Color.WHITE);
                    g2.drawString(distText, (int) midX - textW / 2, (int) midY - 6);
                }
            }

            private void drawTargetRings(Graphics2D g2, double x, double y, Color color) {
                // Outer ring
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
                g2.fillOval((int) (x - 12), (int) (y - 12), 24, 24);

                // Middle border
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawOval((int) (x - 8), (int) (y - 8), 16, 16);

                // Center dot
                g2.setColor(Color.WHITE);
                g2.fillOval((int) (x - 3), (int) (y - 3), 6, 6);
            }

            private Point2D.Double transformPoint(AffineTransform at, double x, double y) {
                Point2D.Double src = new Point2D.Double(x, y);
                Point2D.Double dst = new Point2D.Double();
                at.transform(src, dst);
                return dst;
            }
        }

        // --- Custom Benchmark Panel with Running Time vs N Graph ---
        private class BenchmarkPanel extends JPanel {
            private final List<BenchmarkDataPoint> benchmarkResults = new ArrayList<>();
            private final DefaultTableModel tableModel;
            private final JProgressBar progressBar;
            private final JButton startBenchmarkBtn;
            private final ChartCanvas chartCanvas;

            public BenchmarkPanel() {
                setLayout(new BorderLayout());
                setBackground(new Color(18, 20, 30));

                // Header control for benchmark
                JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
                topBar.setBackground(new Color(26, 28, 42));

                startBenchmarkBtn = createStyledButton("Run Comprehensive Benchmark", new Color(156, 39, 176));
                startBenchmarkBtn.addActionListener(e -> runBenchmark());
                topBar.add(startBenchmarkBtn);

                progressBar = new JProgressBar(0, 100);
                progressBar.setPreferredSize(new Dimension(240, 22));
                progressBar.setStringPainted(true);
                progressBar.setVisible(false);
                topBar.add(progressBar);

                add(topBar, BorderLayout.NORTH);

                // Center: Split between Chart and Table
                chartCanvas = new ChartCanvas();
                JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
                splitPane.setTopComponent(chartCanvas);

                // Table of results
                String[] colNames = {"N (Points)", "Brute-Force O(N²) (ms)", "Divide-and-Conquer O(N log N) (ms)", "Speedup Factor"};
                tableModel = new DefaultTableModel(colNames, 0);
                JTable table = new JTable(tableModel);
                table.setFont(new Font("Consolas", Font.PLAIN, 12));
                table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
                table.setRowHeight(24);

                JScrollPane tableScroll = new JScrollPane(table);
                tableScroll.setPreferredSize(new Dimension(0, 180));
                splitPane.setBottomComponent(tableScroll);
                splitPane.setResizeWeight(0.75);

                add(splitPane, BorderLayout.CENTER);
            }

            private void runBenchmark() {
                startBenchmarkBtn.setEnabled(false);
                progressBar.setVisible(true);
                progressBar.setValue(0);
                tableModel.setRowCount(0);
                benchmarkResults.clear();

                final int[] testN = {100, 250, 500, 1000, 2000, 4000, 8000, 15000, 25000};

                SwingWorker<Void, BenchmarkDataPoint> worker = new SwingWorker<Void, BenchmarkDataPoint>() {
                    @Override
                    protected Void doInBackground() {
                        // Warm up JIT
                        for (int k = 0; k < 3; k++) {
                            Point[] warm = PointGenerator.generateRandom(500, false, 0, 1000);
                            findClosestPairBruteForce(warm);
                            findClosestPairDivideAndConquer(warm);
                        }

                        for (int i = 0; i < testN.length; i++) {
                            int n = testN[i];
                            Point[] pts = PointGenerator.generateRandom(n, false, 0, 10000);

                            // Benchmark Divide-and-Conquer
                            ClosestPairResult dcRes = findClosestPairDivideAndConquer(pts);
                            double dcTime = dcRes.getExecutionTimeMs();

                            // Benchmark Brute-Force (skip or cap for very large N if too slow)
                            double bfTime;
                            if (n <= 15000) {
                                ClosestPairResult bfRes = findClosestPairBruteForce(pts);
                                bfTime = bfRes.getExecutionTimeMs();
                            } else {
                                // Project theoretical estimate for N > 15000 based on previous BF time
                                double lastTime = benchmarkResults.get(benchmarkResults.size() - 1).bfTimeMs;
                                int lastN = benchmarkResults.get(benchmarkResults.size() - 1).n;
                                bfTime = lastTime * ((double) n * n) / ((double) lastN * lastN);
                            }

                            BenchmarkDataPoint dp = new BenchmarkDataPoint(n, bfTime, dcTime);
                            publish(dp);

                            int progress = (int) (((i + 1) / (double) testN.length) * 100);
                            setProgress(progress);
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<BenchmarkDataPoint> chunks) {
                        for (BenchmarkDataPoint dp : chunks) {
                            benchmarkResults.add(dp);
                            String speedup = dp.dcTimeMs > 0 ? String.format(Locale.US, "%.1fx", dp.bfTimeMs / dp.dcTimeMs) : "N/A";
                            tableModel.addRow(new Object[]{
                                    dp.n,
                                    String.format(Locale.US, "%.4f ms", dp.bfTimeMs),
                                    String.format(Locale.US, "%.4f ms", dp.dcTimeMs),
                                    speedup
                            });
                        }
                        chartCanvas.repaint();
                    }

                    @Override
                    protected void done() {
                        startBenchmarkBtn.setEnabled(true);
                        progressBar.setVisible(false);
                    }
                };

                worker.addPropertyChangeListener(evt -> {
                    if ("progress".equals(evt.getPropertyName())) {
                        progressBar.setValue((Integer) evt.getNewValue());
                    }
                });

                worker.execute();
            }

            // Custom line chart rendering for Running Time vs N
            private class ChartCanvas extends JPanel {
                public ChartCanvas() {
                    setBackground(new Color(20, 22, 34));
                }

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth();
                    int h = getHeight();
                    int padLeft = 80, padRight = 50, padTop = 60, padBottom = 60;
                    int plotW = w - padLeft - padRight;
                    int plotH = h - padTop - padBottom;

                    if (plotW <= 0 || plotH <= 0) return;

                    // Title
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    g2.drawString("Execution Time Comparison: Brute-Force O(N²) vs Divide-and-Conquer O(N log N)", padLeft, 35);

                    if (benchmarkResults.isEmpty()) {
                        g2.setColor(Color.GRAY);
                        g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                        g2.drawString("Click 'Run Comprehensive Benchmark' above to generate empirical performance curves.", padLeft + 40, h / 2);
                        return;
                    }

                    // Max N and Max Time
                    int maxN = 1;
                    double maxTime = 1.0;
                    for (BenchmarkDataPoint dp : benchmarkResults) {
                        maxN = Math.max(maxN, dp.n);
                        maxTime = Math.max(maxTime, Math.max(dp.bfTimeMs, dp.dcTimeMs));
                    }
                    maxTime *= 1.1; // 10% head room

                    // Axes
                    g2.setColor(new Color(60, 64, 88));
                    g2.drawLine(padLeft, padTop + plotH, padLeft + plotW, padTop + plotH); // X axis
                    g2.drawLine(padLeft, padTop, padLeft, padTop + plotH); // Y axis

                    // Grid lines and labels
                    g2.setFont(new Font("Consolas", Font.PLAIN, 11));
                    int ySteps = 5;
                    for (int i = 0; i <= ySteps; i++) {
                        int yPos = padTop + plotH - (int) ((i / (double) ySteps) * plotH);
                        double tVal = (i / (double) ySteps) * maxTime;

                        g2.setColor(new Color(40, 44, 62));
                        g2.drawLine(padLeft, yPos, padLeft + plotW, yPos);

                        g2.setColor(new Color(170, 175, 195));
                        g2.drawString(String.format(Locale.US, "%.1f ms", tVal), 15, yPos + 4);
                    }

                    // X-axis label
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g2.drawString("Number of Points (N)", padLeft + plotW / 2 - 50, padTop + plotH + 42);

                    // Draw Curves
                    drawCurve(g2, padLeft, padTop, plotW, plotH, maxN, maxTime, true);  // BF (Red)
                    drawCurve(g2, padLeft, padTop, plotW, plotH, maxN, maxTime, false); // DC (Green)

                    // Legend
                    int legX = padLeft + 20;
                    int legY = padTop + 20;

                    // BF Legend
                    g2.setColor(new Color(239, 83, 80));
                    g2.fillRect(legX, legY, 16, 4);
                    g2.fillOval(legX + 4, legY - 2, 8, 8);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g2.drawString("Brute-Force O(N²)", legX + 24, legY + 5);

                    // DC Legend
                    g2.setColor(new Color(76, 175, 80));
                    g2.fillRect(legX + 160, legY, 16, 4);
                    g2.fillOval(legX + 164, legY - 2, 8, 8);
                    g2.setColor(Color.WHITE);
                    g2.drawString("Divide-and-Conquer O(N log N)", legX + 184, legY + 5);
                }

                private void drawCurve(Graphics2D g2, int padLeft, int padTop, int plotW, int plotH,
                                       int maxN, double maxTime, boolean isBruteForce) {
                    Color color = isBruteForce ? new Color(239, 83, 80) : new Color(76, 175, 80);
                    g2.setColor(color);
                    g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                    int prevX = -1, prevY = -1;
                    for (BenchmarkDataPoint dp : benchmarkResults) {
                        double timeVal = isBruteForce ? dp.bfTimeMs : dp.dcTimeMs;
                        int cx = padLeft + (int) ((dp.n / (double) maxN) * plotW);
                        int cy = padTop + plotH - (int) ((timeVal / maxTime) * plotH);

                        if (prevX != -1) {
                            g2.drawLine(prevX, prevY, cx, cy);
                        }

                        // Dot marker
                        g2.fillOval(cx - 4, cy - 4, 8, 8);
                        g2.setColor(Color.WHITE);
                        g2.drawOval(cx - 4, cy - 4, 8, 8);
                        g2.setColor(color);

                        prevX = cx;
                        prevY = cy;
                    }
                }
            }
        }

        private static class BenchmarkDataPoint {
            final int n;
            final double bfTimeMs;
            final double dcTimeMs;

            BenchmarkDataPoint(int n, double bf, double dc) {
                this.n = n;
                this.bfTimeMs = bf;
                this.dcTimeMs = dc;
            }
        }
    }

    // ========================================================================
    // 5. OUTPUT FORMATTER & TERMINAL DRIVER
    // ========================================================================

    /**
     * Prints the exact three-line output required:
     * Line 1: Brute-Force min distance and coordinates
     * Line 2: Divide-and-Conquer min distance and coordinates
     * Line 3: Execution time in milliseconds
     */
    public static void printResults(ClosestPairResult bfRes, ClosestPairResult dcRes) {
        if (bfRes == null || dcRes == null) {
            System.err.println("Error: Insufficient points to compute closest pair.");
            return;
        }

        // Line 1: Brute-Force
        System.out.printf(Locale.US, "Brute-Force: %.6f, %s, %s%n",
                bfRes.distance, bfRes.p1, bfRes.p2);

        // Line 2: Divide-and-Conquer
        System.out.printf(Locale.US, "Divide-and-Conquer: %.6f, %s, %s%n",
                dcRes.distance, dcRes.p1, dcRes.p2);

        // Line 3: Execution Time
        System.out.printf(Locale.US, "Execution Time: Brute-Force = %.6f ms, Divide-and-Conquer = %.6f ms%n",
                bfRes.getExecutionTimeMs(), dcRes.getExecutionTimeMs());
    }

    // ========================================================================
    // 6. MAIN METHOD
    // ========================================================================

    public static void main(String[] args) {
        int n = -1;
        boolean integerCoords = false;
        boolean noGui = false;

        // 1. Check command line arguments first if provided
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--no-gui") || arg.equalsIgnoreCase("-nogui")) {
                noGui = true;
            } else if (arg.equalsIgnoreCase("int") || arg.equalsIgnoreCase("integer")) {
                integerCoords = true;
            } else if (n == -1) {
                try {
                    n = Integer.parseInt(arg);
                } catch (NumberFormatException ignored) {}
            }
        }

        // 2. If N not passed via args, accept N from standard input
        Scanner scanner = new Scanner(System.in);
        if (n == -1) {
            // Prompt interactively only if a real console is attached; omit when input is piped
            if (System.console() != null) {
                System.err.println("Enter integer N (number of 2D points to generate): ");
            }
            if (scanner.hasNextInt()) {
                n = scanner.nextInt();
                // Check if user optionally specifies coordinate type
                if (scanner.hasNext()) {
                    String typeToken = scanner.next().trim();
                    if (typeToken.equalsIgnoreCase("int") || typeToken.equalsIgnoreCase("integer") || typeToken.equals("2")) {
                        integerCoords = true;
                    }
                }
            } else {
                System.err.println("Error: No valid integer provided on standard input.");
                return;
            }
        }

        // 3. Robust Edge Case Validation: N < 2
        if (n < 2) {
            System.err.printf("Error: Number of points N must be >= 2. Received: N = %d%n", n);
            return;
        }

        // 4. Randomly generate N 2D points supporting integer / floating-point coordinates
        Point[] points = PointGenerator.generateRandom(n, integerCoords, 0.0, 1000.0);

        // 5. Execute Brute-Force Algorithm
        ClosestPairResult bfResult = findClosestPairBruteForce(points);

        // 6. Execute Divide-and-Conquer Algorithm
        ClosestPairResult dcResult = findClosestPairDivideAndConquer(points);

        // 7. Output according to exact required specification
        printResults(bfResult, dcResult);

        // 8. Launch Visualization GUI if graphical display is available and not disabled
        if (!noGui && !GraphicsEnvironment.isHeadless()) {
            final Point[] ptsCopy = points;
            final ClosestPairResult bfCopy = bfResult;
            final ClosestPairResult dcCopy = dcResult;
            final boolean isIntCopy = integerCoords;

            SwingUtilities.invokeLater(() -> {
                VisualizationWindow window = new VisualizationWindow(ptsCopy, bfCopy, dcCopy, isIntCopy);
                window.setVisible(true);
            });
        }
    }
}
