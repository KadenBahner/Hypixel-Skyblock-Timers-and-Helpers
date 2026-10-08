package timerbotmain;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.sound.sampled.*;
import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;


public class HypixelTimerApp extends JFrame {
	private static final String DEVELOPER_API_KEY = loadApiKey();

	private static String loadApiKey() {
	    java.util.Properties p = new java.util.Properties();

	    File f = new File("secrets.properties");
	    if (!f.isFile()) {
	        try {
	            File jar = new File(HypixelTimerApp.class.getProtectionDomain()
	                    .getCodeSource().getLocation().toURI());
	            File parent = jar.getParentFile();
	            if (parent != null) {
	                File alt = new File(parent, "secrets.properties");
	                if (alt.isFile()) f = alt;
	            }
	        } catch (Exception ignored) {}
	    }

	    if (!f.isFile()) return "";

	    try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
	        p.load(in);
	        String k = p.getProperty("hypixel.api.key");
	        return k == null ? "" : k.trim();
	    } catch (Exception e) {
	        return "";
	    }
	}
	private boolean ensureApiKey() {
	    if (DEVELOPER_API_KEY != null && !DEVELOPER_API_KEY.isEmpty()) return true;

	    JOptionPane.showMessageDialog(this,
	            "<html><body style='width:420px;font-family:Segoe UI'>"
	            + "<b>Missing API key</b><br><br>"
	            + "Create a file called <b>secrets.properties</b> in the same folder "
	            + "you launched the app from, containing exactly one line:<br><br>"
	            + "<code>hypixel.api.key=your-key-here</code><br><br>"
	            + "Get a free key at <b>developer.hypixel.net</b>."
	            + "</body></html>",
	            "Missing API Key", JOptionPane.WARNING_MESSAGE);
	    return false;
	}
    // ===== DEVELOPER CONFIG =====

    private static final java.util.Set<String> FARMING_CROPS = java.util.Set.of(
            "WHEAT", "CARROT_ITEM", "POTATO_ITEM", "PUMPKIN", "MELON",
            "MUSHROOM_COLLECTION", "CACTUS", "SUGAR_CANE", "NETHER_STALK", "SEEDS"
    );
    private static final java.util.Set<String> MINING_BLOCKS = java.util.Set.of(
            "COBBLESTONE", "COAL", "IRON_INGOT", "GOLD_INGOT", "DIAMOND",
            "LAPIS_LAZULI", "EMERALD", "REDSTONE", "GLOWSTONE_DUST", "OBSIDIAN",
            "GRAVEL", "SAND", "MITHRIL_ORE", "HARD_STONE", "GEMSTONE_COLLECTION", "NETHERRACK"
    );

    private static final Map<String, Long> FORGE_DURATIONS = new java.util.HashMap<>();
    static {
        FORGE_DURATIONS.put("REFINED_MITHRIL",     TimeUnit.HOURS.toMillis(6));
        FORGE_DURATIONS.put("REFINED_TITANIUM",    TimeUnit.HOURS.toMillis(12));
        FORGE_DURATIONS.put("REFINED_DIAMOND",     TimeUnit.HOURS.toMillis(8));
        FORGE_DURATIONS.put("GOLDEN_PLATE",        TimeUnit.HOURS.toMillis(6));
        FORGE_DURATIONS.put("MITHRIL_PLATE",       TimeUnit.HOURS.toMillis(18));
        FORGE_DURATIONS.put("PERFECT_PLATE",       TimeUnit.MINUTES.toMillis(30));
        FORGE_DURATIONS.put("BEJEWELED_HANDLE",    TimeUnit.HOURS.toMillis(22));
        FORGE_DURATIONS.put("DRILL_MOTOR",         TimeUnit.MINUTES.toMillis(30));
        FORGE_DURATIONS.put("FUEL_TANK",           TimeUnit.HOURS.toMillis(10));
        FORGE_DURATIONS.put("GEMSTONE_MIXTURE",    TimeUnit.HOURS.toMillis(4));
        FORGE_DURATIONS.put("PURE_MITHRIL",        TimeUnit.HOURS.toMillis(4));
        FORGE_DURATIONS.put("AMALGAMATED_CRYSTAL", TimeUnit.HOURS.toMillis(2));
        FORGE_DURATIONS.put("GLACITE_JEWEL",       TimeUnit.HOURS.toMillis(1));
        FORGE_DURATIONS.put("SORROW",              TimeUnit.HOURS.toMillis(3));
    }

    private static final Map<String, Map<String, Integer>> FORGE_RECIPES = new java.util.HashMap<>();
    static {
        Map<String, Integer> m;
        m = new java.util.HashMap<>(); m.put("ENCHANTED_MITHRIL", 160); FORGE_RECIPES.put("REFINED_MITHRIL", m);
        m = new java.util.HashMap<>(); m.put("ENCHANTED_TITANIUM", 16); FORGE_RECIPES.put("REFINED_TITANIUM", m);
        m = new java.util.HashMap<>(); m.put("ENCHANTED_DIAMOND_BLOCK", 2); FORGE_RECIPES.put("REFINED_DIAMOND", m);
        m = new java.util.HashMap<>();
        m.put("ENCHANTED_GOLD_BLOCK", 2); m.put("GLACITE_JEWEL", 5); m.put("REFINED_DIAMOND", 1);
        FORGE_RECIPES.put("GOLDEN_PLATE", m);
        m = new java.util.HashMap<>();
        m.put("REFINED_MITHRIL", 5); m.put("GOLDEN_PLATE", 1); m.put("ENCHANTED_IRON_BLOCK", 1); m.put("REFINED_TITANIUM", 1);
        FORGE_RECIPES.put("MITHRIL_PLATE", m);
        m = new java.util.HashMap<>();
        m.put("UMBER_PLATE", 1); m.put("TUNGSTEN_PLATE", 1); m.put("MITHRIL_PLATE", 1);
        FORGE_RECIPES.put("PERFECT_PLATE", m);
        m = new java.util.HashMap<>(); m.put("GLACITE_JEWEL", 3); FORGE_RECIPES.put("BEJEWELED_HANDLE", m);
        m = new java.util.HashMap<>();
        m.put("ENCHANTED_IRON_BLOCK", 1); m.put("ENCHANTED_REDSTONE_BLOCK", 3);
        m.put("GOLDEN_PLATE", 1); m.put("TREASURITE", 10);
        FORGE_RECIPES.put("DRILL_MOTOR", m);
        m = new java.util.HashMap<>(); m.put("ENCHANTED_COAL_BLOCK", 2); FORGE_RECIPES.put("FUEL_TANK", m);
        m = new java.util.HashMap<>();
        m.put("FINE_JADE_GEM", 4); m.put("FINE_AMBER_GEM", 4);
        m.put("FINE_AMETHYST_GEM", 4); m.put("FINE_SAPPHIRE_GEM", 4); m.put("SLUDGE_JUICE", 320);
        FORGE_RECIPES.put("GEMSTONE_MIXTURE", m);
        m = new java.util.HashMap<>(); m.put("REFINED_MITHRIL", 2); FORGE_RECIPES.put("PURE_MITHRIL", m);
        m = new java.util.HashMap<>();
        m.put("REFINED_TITANIUM", 1); m.put("REFINED_DIAMOND", 1); m.put("GLACITE_JEWEL", 1);
        FORGE_RECIPES.put("AMALGAMATED_CRYSTAL", m);
    }

    // ===== KAT RECIPE DATA =====
    private static class KatRecipe {
        final long coinCost;
        final long durationMillis;
        final String[] materials;
        KatRecipe(long coinCost, long durationMillis, String... materials) {
            this.coinCost = coinCost;
            this.durationMillis = durationMillis;
            this.materials = materials;
        }
    }

    private static final String[] KAT_RARITIES = {
            "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"
    };

    private static final Map<String, List<KatRecipe>> KAT_RECIPES = new LinkedHashMap<>();
    static {
        List<KatRecipe> rabbit = new ArrayList<>();
        rabbit.add(new KatRecipe(5_000L, TimeUnit.MINUTES.toMillis(5),
                "512x Raw Rabbit", "256x Rabbit Hide", "256x Rabbit Foot"));
        rabbit.add(new KatRecipe(10_000L, TimeUnit.MINUTES.toMillis(10),
                "8x Enchanted Raw Rabbit", "4x Enchanted Rabbit Hide", "4x Enchanted Rabbit Foot"));
        rabbit.add(new KatRecipe(25_000L, TimeUnit.MINUTES.toMillis(15),
                "16x Enchanted Raw Rabbit", "8x Enchanted Rabbit Hide", "8x Enchanted Rabbit Foot"));
        rabbit.add(new KatRecipe(50_000L, TimeUnit.MINUTES.toMillis(30),
                "32x Enchanted Raw Rabbit", "16x Enchanted Rabbit Hide", "16x Enchanted Rabbit Foot"));
        rabbit.add(new KatRecipe(100_000L, TimeUnit.HOURS.toMillis(1),
                "64x Enchanted Raw Rabbit", "32x Enchanted Rabbit Hide", "32x Enchanted Rabbit Foot",
                "1x Chocolate Syringe"));
        KAT_RECIPES.put("RABBIT", rabbit);

        List<KatRecipe> enderman = new ArrayList<>();
        enderman.add(new KatRecipe(10_000L, TimeUnit.DAYS.toMillis(1)));
        enderman.add(new KatRecipe(50_000L, TimeUnit.DAYS.toMillis(2)));
        enderman.add(new KatRecipe(100_000L, TimeUnit.DAYS.toMillis(6)));
        enderman.add(new KatRecipe(40_000_000L, TimeUnit.DAYS.toMillis(12),
                "8x Enchanted Eye of Ender"));
        enderman.add(new KatRecipe(1_000_000L, TimeUnit.HOURS.toMillis(1),
                "1x Enderman Cortex Rewriter"));
        KAT_RECIPES.put("ENDERMAN", enderman);

        List<KatRecipe> mithrilGolem = new ArrayList<>();
        mithrilGolem.add(new KatRecipe(5_000L, TimeUnit.MINUTES.toMillis(5)));
        mithrilGolem.add(new KatRecipe(10_000L, TimeUnit.MINUTES.toMillis(10)));
        mithrilGolem.add(new KatRecipe(15_000L, TimeUnit.MINUTES.toMillis(15)));
        mithrilGolem.add(new KatRecipe(30_000L, TimeUnit.MINUTES.toMillis(30)));
        mithrilGolem.add(new KatRecipe(1_000_000L, TimeUnit.HOURS.toMillis(1)));
        KAT_RECIPES.put("MITHRIL_GOLEM", mithrilGolem);
    }

    private static final String FONT_UI    = "Segoe UI";
    private static final String FONT_PIXEL = "Monospaced";

    private enum Waveform { SINE, SQUARE }
    private enum NavIconType { HOME, FORGE, KAT, CUSTOM, ACTIVE, STATS, SETTINGS, PROFITS, TIMER, HELP }
    private final Preferences prefs = Preferences.userNodeForPackage(HypixelTimerApp.class);
    private boolean darkMode;
    private String  soundChoice;
    private String  lastUsername;
    private int    volume;
    private String customSongPath;

    private double totalProfitCoins = 0;
    private final Map<String, Double> perRecipeProfit = new TreeMap<>();

    private Color cBg, cPanel, cPanelAlt, cText, cSubtext, cBorder,
                  cSidebarBg, cSidebarBorder, cStatusBg, cAccentStripe;

    private void updateThemeColors() {
        if (darkMode) {
            cBg            = new Color(24, 24, 30);
            cPanel         = new Color(38, 38, 46);
            cPanelAlt      = new Color(46, 46, 58);
            cText          = new Color(238, 238, 244);
            cSubtext       = new Color(160, 160, 172);
            cBorder        = new Color(58, 58, 68);
            cSidebarBg     = new Color(18, 18, 24);
            cSidebarBorder = new Color(40, 40, 50);
            cStatusBg      = new Color(28, 28, 34);
            cAccentStripe  = new Color(60, 130, 220);
        } else {
            cBg            = new Color(246, 247, 251);
            cPanel         = new Color(255, 255, 255);
            cPanelAlt      = new Color(242, 244, 250);
            cText          = new Color(24, 24, 30);
            cSubtext       = new Color(108, 110, 122);
            cBorder        = new Color(218, 220, 228);
            cSidebarBg     = new Color(238, 240, 246);
            cSidebarBorder = new Color(214, 216, 224);
            cStatusBg      = new Color(232, 234, 240);
            cAccentStripe  = new Color(60, 130, 220);
        }
    }

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ExecutorService soundExecutor = Executors.newSingleThreadExecutor();
    private final List<ActiveTimer> activeTimers = new CopyOnWriteArrayList<>();
    private final List<CustomButtonDef> customButtons = new ArrayList<>();

    private final AtomicLong soundToken = new AtomicLong(0);
    // Recipe key → number of completed timers waiting on Bazaar prices.
    private final Map<String, Integer> pendingProfitRecords = new ConcurrentHashMap<>();
    private final Object statsLock = new Object();
    private final Map<String, TimerStats> timerStats = new TreeMap<>();
    private final Map<String, Long> timerDurations = new TreeMap<>();
    private long accumulatedActivityMillis = 0;
    private long activityStartMillis = 0;
    private volatile boolean statsDirty = true;
    private volatile boolean timerStatsDirty = true;
    private long lastPeriodicSave = 0;

    private static class TimerStats {
        int startedCount;
        int completedCount;
        TimerStats() {}
        TimerStats(int s, int c) { startedCount = s; completedCount = c; }
    }

    private static class ActiveTimer {
        final String label;
        final long startTimeMillis;
        final long endTimeMillis;
        final ScheduledFuture<?> future;
        ActiveTimer(String label, long startTimeMillis, long endTimeMillis, ScheduledFuture<?> future) {
            this.label = label;
            this.startTimeMillis = startTimeMillis;
            this.endTimeMillis = endTimeMillis;
            this.future = future;
        }
    }

    private static class CustomButtonDef {
        final String name;
        final long durationMillis;
        CustomButtonDef(String name, long durationMillis) {
            this.name = name;
            this.durationMillis = durationMillis;
        }
    }

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private CompletableFuture<Void> currentFetch;

    private volatile Map<String, Double> bazaarBuyPrices = new java.util.HashMap<>();
    private volatile Map<String, Double> bazaarSellPrices = new java.util.HashMap<>();
    private volatile long bazaarLastUpdated = 0;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screens = new JPanel(cardLayout);
    private final JLabel statusBar = new JLabel(" Ready");
    private final DefaultListModel<String> activeTimersModel = new DefaultListModel<>();
    private final JList<String> activeTimersList = new JList<>(activeTimersModel);
    private JTextField forgeUsernameField;
    private FlatButton forgeRefreshBtn;
    private JLabel forgeStatusLabel;
    private JPanel activeForgesContainer;
    private final List<ForgeRowRefs> forgeRows = new CopyOnWriteArrayList<>();

    private static class ForgeRowRefs {
        final long endTime;
        final long startTime;
        final JLabel infoLabel;
        final JLabel badgeLabel;
        ForgeRowRefs(long startTime, long endTime, JLabel infoLabel, JLabel badgeLabel) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.infoLabel = infoLabel;
            this.badgeLabel = badgeLabel;
        }
    }

    private static class ActiveForge {
        String type;
        String displayName;
        long startTime;
        long endTime;
    }

    private final List<FlatButton> flatButtons = new ArrayList<>();
    private final List<NavButton> navButtons = new ArrayList<>();
    private final List<StatCard> statCards = new ArrayList<>();
    private final List<TabButton> tabButtons = new ArrayList<>();
    private final List<HeroCard> heroCards = new CopyOnWriteArrayList<>();
    private final List<RoundedInputWrapper> roundedWrappers = new CopyOnWriteArrayList<>();

    private JPanel sidebar;
    private JPanel customButtonsContainer;
    private JPanel historyListContainer;
    private final List<JScrollPane> themedScrollPanes = new ArrayList<>();
    private JLabel slivvyLabel;
    private JLabel statActiveValue, statCompletedValue, statTimeValue;
    private JLabel heroProfitValue, heroProfitSub;
    private JLabel heroBestFlipName, heroBestFlipSub;
    private JLabel nextUpTitleLabel, nextUpTimeLabel;
    private JPanel nextUpBox;
    private JRadioButton lightRadio, darkRadio;
    private JComboBox<String> settingsSoundCombo;
    private JTextField usernameField;
    private FlatButton fetchStatsBtn;
    private JPanel statsContainer;

    private JPanel timerStatsContainer;
    private String currentStatTab = "PROFILE";

    private JTextField katUsernameField;
    private FlatButton katRefreshBtn;
    private JLabel katStatusLabel;
    private JPanel activeKatContainer;

    private JPanel profitsContainer;
    private JPanel profitsSummaryPanel;
    private JPanel profitsTablePanel;
    private JComboBox<String> calcRecipeCombo;
    private JSpinner calcQtySpinner;
    private JSpinner calcSlotsSpinner;
    private CalcResultCard calcCardCost, calcCardOut, calcCardProfit;
    private CalcResultCard calcCardTime, calcCardPerHour, calcCardRoi;
    private JLabel profitTotalLabel;
    private FlatButton refreshBazaarBtn;
    private JLabel bazaarStatusLabel;

    private Timer rainbowTimer;
    private float hue = 0f;
    private String currentScreen = "HOME";
    private TrayIcon trayIcon;
    private boolean  trayAvailable = false;

    private JPanel devConsolePanel;
    private JTextArea devConsoleOutput;
    private JTextField devConsoleInput;
    private boolean devConsoleVisible = false;
    private static final Color C_GOLD       = new Color(255, 200, 90);
    private static final Color C_GREEN      = new Color(90, 225, 140);
    private static final Color C_RED        = new Color(240, 100, 100);
    private static final Color C_BLUE       = new Color(120, 200, 255);
    private static final Color C_ORANGE     = new Color(255, 155, 90);

    public HypixelTimerApp() {
        darkMode       = prefs.getBoolean("darkMode", true);
        soundChoice    = prefs.get("sound", "Chime");
        lastUsername   = prefs.get("lastUsername", "");
        volume         = prefs.getInt("volume", 80);
        customSongPath = prefs.get("customSongPath", "");
        totalProfitCoins = prefs.getDouble("profit.total", 0);
        loadPerRecipeProfit();
        loadPendingProfits();
        updateThemeColors();
        loadCustomButtons();
        loadStats();

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        setTitle("Hypixel Skyblock Timer");
        setIconImage(createAppIcon());
        setSize(1200, 820);
        setMinimumSize(new Dimension(1050, 720));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initSystemTray();
        soundChoice    = prefs.get("sound", "Dopamine");
        getContentPane().setBackground(cBg);

        statusBar.setOpaque(true);
        statusBar.setBorder(new EmptyBorder(8, 16, 8, 16));
        statusBar.setFont(new Font(FONT_UI, Font.PLAIN, 12));

        screens.setOpaque(false);
        screens.add(createHomeScreen(),        "HOME");
        screens.add(createForgeTrackerScreen(),"FORGE");
        screens.add(createProfitsScreen(),     "PROFITS");
        screens.add(createStatTrackingScreen(),"STATS");
        screens.add(createKatScreen(),         "KAT");
        screens.add(createCustomScreen(),      "CUSTOM");
        screens.add(createActiveScreen(),      "ACTIVE");
        screens.add(createSettingsScreen(),    "SETTINGS");
        screens.add(createHelpScreen(),        "HELP");
        sidebar = createSidebar();
        devConsolePanel = buildDevConsole();

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.add(screens, BorderLayout.CENTER);

        JPanel southStack = new JPanel(new BorderLayout());
        southStack.setOpaque(false);
        southStack.add(devConsolePanel, BorderLayout.CENTER);
        southStack.add(statusBar, BorderLayout.SOUTH);
        contentWrapper.add(southStack, BorderLayout.SOUTH);
        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(contentWrapper, BorderLayout.CENTER);

        new Timer(1000, e -> {
            if (trayAvailable && trayIcon != null) {
                if (activeTimers.isEmpty()) {
                    trayIcon.setToolTip("Hypixel SkyBlock Timer");
                } else {
                    ActiveTimer soon = activeTimers.get(0);
                    for (ActiveTimer t : activeTimers)
                        if (t.endTimeMillis < soon.endTimeMillis) soon = t;
                    long rem = soon.endTimeMillis - System.currentTimeMillis();
                    trayIcon.setToolTip("Next: " + soon.label + "  (" + formatDuration(rem) + ")");
                }
            }
            refreshActiveTimersList();
            if ("HOME".equals(currentScreen)) refreshStatsUI();
            if (!forgeRows.isEmpty()) tickForgeRows();
            if ("STATS".equals(currentScreen)
                    && "TIMER_STATS".equals(currentStatTab)
                    && (timerStatsDirty || !activeTimers.isEmpty())) {
                refreshTimerStatsUI();
            }
            if (System.currentTimeMillis() - lastPeriodicSave > 30_000) {
                lastPeriodicSave = System.currentTimeMillis();
                synchronized (statsLock) {
                    long now = System.currentTimeMillis();
                    long frozenSnapshot = accumulatedActivityMillis
                            + (activityStartMillis > 0 ? now - activityStartMillis : 0);
                    prefs.putLong("stats.activity.accumulated", frozenSnapshot);
                    prefs.putLong("stats.activity.start", 0L);
                    for (Map.Entry<String, TimerStats> en : timerStats.entrySet()) {
                        String enc = URLEncoder.encode(en.getKey(), StandardCharsets.UTF_8);
                        prefs.putInt("stats.timer." + enc + ".started",   en.getValue().startedCount);
                        prefs.putInt("stats.timer." + enc + ".completed", en.getValue().completedCount);
                    }
                    for (Map.Entry<String, Long> en : timerDurations.entrySet()) {
                        String enc = URLEncoder.encode(en.getKey(), StandardCharsets.UTF_8);
                        prefs.putLong("stats.timer." + enc + ".duration", en.getValue());
                    }
                    saveProfitData();
                    savePendingProfits();
                    try { prefs.flush(); } catch (BackingStoreException ignored) {}
                }
            }
        }).start();

        new Timer(16, e -> {
            if (!isVisible()) return;
            for (HeroCard hc : heroCards) {
                if (hc.isShowing()) hc.repaint();
            }
            for (NavButton nb : navButtons) {
                nb.tickAnimation();
            }
        }).start();

        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) {
                trySetDarkTitleBar();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "toggleDevConsole");
        getRootPane().getActionMap().put("toggleDevConsole", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                toggleDevConsole();
            }
        });
        startRainbow();
        applyTheme();
        showScreen("HOME");
        fetchBazaarPrices();
        restoreActiveTimers();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                synchronized (statsLock) {
                    if (activityStartMillis > 0) {
                        accumulatedActivityMillis += System.currentTimeMillis() - activityStartMillis;
                        activityStartMillis = 0;
                    }
                    saveActiveTimers();
                    saveStats();
                    saveProfitData();
                    savePendingProfits();
                    try { prefs.flush(); } catch (BackingStoreException ignored) {}
                }
                stopSound();
                if (rainbowTimer != null) rainbowTimer.stop();
                scheduler.shutdownNow();
                soundExecutor.shutdownNow();
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                synchronized (statsLock) {
                    if (activityStartMillis > 0) {
                        accumulatedActivityMillis += System.currentTimeMillis() - activityStartMillis;
                        activityStartMillis = 0;
                    }
                    saveActiveTimers();
                    saveStats();
                    saveProfitData();
                    savePendingProfits();
                    prefs.flush();
                }
            } catch (Exception ignored) {}
        }, "timer-shutdown-save"));
    }

    // ===== DARK TITLE BAR =====
    private void trySetDarkTitleBar() {
        if (!darkMode) return;
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) return;

        new Thread(() -> {
            try {
                Thread.sleep(300);
                long hwnd = getWindowHandle();
                if (hwnd == 0) return;

                String ps =
                    "$sig = '[DllImport(\"dwmapi.dll\")] public static extern int DwmSetWindowAttribute(IntPtr h,int a,ref int v,int s);'; " +
                    "$t = Add-Type -MemberDefinition $sig -Name D -Namespace W -PassThru; " +
                    "$v = 1; " +
                    "[W.D]::DwmSetWindowAttribute([IntPtr]" + hwnd + ", 20, [ref]$v, 4) | Out-Null; " +
                    "[W.D]::DwmSetWindowAttribute([IntPtr]" + hwnd + ", 19, [ref]$v, 4) | Out-Null";

                ProcessBuilder pb = new ProcessBuilder(
                        "powershell", "-NoProfile", "-NonInteractive", "-Command", ps);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor(3, TimeUnit.SECONDS);
            } catch (Throwable ignored) {}
        }, "dark-titlebar").start();
    }

    private long getWindowHandle() {
        try {
            Object peer;
            try {
                java.lang.reflect.Method m = java.awt.Window.class.getDeclaredMethod("getPeer");
                m.setAccessible(true);
                peer = m.invoke(this);
            } catch (Exception e1) {
                java.lang.reflect.Field f = java.awt.Window.class.getDeclaredField("peer");
                f.setAccessible(true);
                peer = f.get(this);
            }
            if (peer == null) return 0;

            Class<?> clazz = peer.getClass();
            java.lang.reflect.Field hwndField = null;
            while (clazz != null && hwndField == null) {
                try {
                    hwndField = clazz.getDeclaredField("hwnd");
                } catch (NoSuchFieldException nsfe) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (hwndField == null) return 0;
            hwndField.setAccessible(true);
            Object h = hwndField.get(peer);
            return (h instanceof Number) ? ((Number) h).longValue() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void saveStats() {
        prefs.putLong("stats.activity.accumulated", accumulatedActivityMillis);
        prefs.putLong("stats.activity.start", activityStartMillis);
        for (Map.Entry<String, TimerStats> e : timerStats.entrySet()) {
            String enc = URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8);
            prefs.putInt("stats.timer." + enc + ".started",   e.getValue().startedCount);
            prefs.putInt("stats.timer." + enc + ".completed", e.getValue().completedCount);
        }
        for (Map.Entry<String, Long> e : timerDurations.entrySet()) {
            String enc = URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8);
            prefs.putLong("stats.timer." + enc + ".duration", e.getValue());
        }
    }

    private static Color shiftHue(Color c, float delta) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hsb[0] = (hsb[0] + delta + 1f) % 1f;
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    private static int katTierIndex(String tier) {
        if (tier == null) return -1;
        for (int i = 0; i < KAT_RARITIES.length; i++) {
            if (KAT_RARITIES[i].equalsIgnoreCase(tier)) return i;
        }
        return -1;
    }

    // ===== ROUNDED INPUT WRAPPER =====
    private class RoundedInputWrapper extends JPanel {
        RoundedInputWrapper(JComponent content, int width, int height) {
            setLayout(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(width, height));
            setMaximumSize(new Dimension(width, height));
            setMinimumSize(new Dimension(width, height));
            add(content, BorderLayout.CENTER);
            roundedWrappers.add(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), arc = 10;
            g2.setColor(darkMode ? new Color(28, 29, 38) : new Color(250, 250, 252));
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.setColor(darkMode ? new Color(72, 74, 86) : new Color(208, 210, 220));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            g2.dispose();
        }
    }

    // ===== DEV CONSOLE =====
    private JPanel buildDevConsole() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(true);
        panel.setBackground(new Color(15, 15, 20));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 130, 220)),
                new EmptyBorder(10, 14, 10, 14)));
        panel.setPreferredSize(new Dimension(0, 240));
        panel.setVisible(false);

        JLabel header = new JLabel("DEV CONSOLE  \u2014  type !help for commands  \u2014  F4 to close");
        header.setFont(new Font(FONT_PIXEL, Font.BOLD, 11));
        header.setForeground(new Color(120, 200, 255));

        devConsoleOutput = new JTextArea();
        devConsoleOutput.setEditable(false);
        devConsoleOutput.setFont(new Font(FONT_PIXEL, Font.PLAIN, 12));
        devConsoleOutput.setBackground(new Color(10, 10, 15));
        devConsoleOutput.setForeground(new Color(180, 220, 180));
        devConsoleOutput.setCaretColor(new Color(180, 220, 180));
        devConsoleOutput.setBorder(new EmptyBorder(6, 8, 6, 8));

        JScrollPane scroll = new JScrollPane(devConsoleOutput);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(40, 40, 50), 1));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(new Color(10, 10, 15));
        installModernScrollbar(scroll);

        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setOpaque(false);

        JLabel prompt = new JLabel(">");
        prompt.setFont(new Font(FONT_PIXEL, Font.BOLD, 13));
        prompt.setForeground(new Color(120, 200, 255));

        devConsoleInput = new JTextField();
        devConsoleInput.setFont(new Font(FONT_PIXEL, Font.PLAIN, 12));
        devConsoleInput.setBackground(new Color(20, 20, 28));
        devConsoleInput.setForeground(new Color(220, 240, 220));
        devConsoleInput.setCaretColor(new Color(220, 240, 220));
        devConsoleInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 130, 220), 1),
                new EmptyBorder(6, 8, 6, 8)));
        devConsoleInput.addActionListener(e -> handleDevCommand(devConsoleInput.getText()));

        inputRow.add(prompt, BorderLayout.WEST);
        inputRow.add(devConsoleInput, BorderLayout.CENTER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(inputRow, BorderLayout.SOUTH);

        devLog("Dev console ready. Type !help for the command list.");
        return panel;
    }

    private void toggleDevConsole() {
        devConsoleVisible = !devConsoleVisible;
        if (devConsolePanel != null) {
            devConsolePanel.setVisible(devConsoleVisible);
        }
        if (devConsoleVisible && devConsoleInput != null) {
            devConsoleInput.requestFocusInWindow();
        }
        revalidate();
        repaint();
    }

    private void devLog(String msg) {
        if (devConsoleOutput == null) return;
        SwingUtilities.invokeLater(() -> {
            devConsoleOutput.append(msg + "\n");
            devConsoleOutput.setCaretPosition(devConsoleOutput.getDocument().getLength());
        });
    }

    private void handleDevCommand(String raw) {
        if (raw == null) return;
        String line = raw.trim();
        if (devConsoleInput != null) devConsoleInput.setText("");
        if (line.isEmpty()) return;

        devLog("> " + line);

        if (!line.startsWith("!")) {
            devLog("Commands must start with !.  Type !help for the list.");
            return;
        }

        String[] parts = line.substring(1).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            devLog("Empty command. Type !help.");
            return;
        }
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "help" -> {
                devLog("Available commands:");
                devLog("  !setprofit <coins>   \u2014 Set total profit to an exact value");
                devLog("  !addprofit <coins>   \u2014 Add to total profit (or use negatives)");
                devLog("  !getprofit           \u2014 Print current total profit");
                devLog("  !resetprofit         \u2014 Reset total profit to 0");
                devLog("  !clear               \u2014 Clear the console output");
                devLog("  !exit                \u2014 Close the dev console");
            }
            case "setprofit" -> {
                if (parts.length < 2) { devLog("Usage: !setprofit <coins>"); break; }
                try {
                    double v = Double.parseDouble(parts[1]);
                    synchronized (statsLock) {
                        totalProfitCoins = v;
                        saveProfitData();
                    }
                    devLog("Total profit set to " + formatCoins(v) + " coins (" + (long) v + ")");
                    SwingUtilities.invokeLater(() -> { refreshStatsUI(); refreshProfitsUI(); });
                } catch (NumberFormatException ex) {
                    devLog("Invalid number: " + parts[1]);
                }
            }
            case "addprofit" -> {
                if (parts.length < 2) { devLog("Usage: !addprofit <coins>"); break; }
                try {
                    double v = Double.parseDouble(parts[1]);
                    synchronized (statsLock) {
                        totalProfitCoins += v;
                        saveProfitData();
                    }
                    devLog("Added " + formatCoins(v) + " coins. New total: "
                            + formatCoins(totalProfitCoins) + " (" + (long) totalProfitCoins + ")");
                    SwingUtilities.invokeLater(() -> { refreshStatsUI(); refreshProfitsUI(); });
                } catch (NumberFormatException ex) {
                    devLog("Invalid number: " + parts[1]);
                }
            }
            case "getprofit" -> {
                devLog("Total profit: " + formatCoins(totalProfitCoins)
                        + " coins (" + (long) totalProfitCoins + ")");
            }
            case "resetprofit" -> {
                synchronized (statsLock) {
                    totalProfitCoins = 0;
                    perRecipeProfit.clear();
                    pendingProfitRecords.clear();
                    saveProfitData();
                    savePendingProfits();
                }
                devLog("Total profit, per-recipe stats, and pending queue reset to 0.");
                SwingUtilities.invokeLater(() -> { refreshStatsUI(); refreshProfitsUI(); });
            }
            case "clear" -> {
                if (devConsoleOutput != null) devConsoleOutput.setText("");
            }
            case "exit" -> toggleDevConsole();
            default -> devLog("Unknown command: !" + cmd + "  (try !help)");
        }
    }

    // ===== SIDEBAR =====
    private JPanel createSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(true);
        side.setPreferredSize(new Dimension(220, 0));

        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.X_AXIS));
        logoPanel.setOpaque(false);
        logoPanel.setBorder(new EmptyBorder(24, 22, 22, 22));

        JLabel logoIcon = new JLabel(new NavIcon(NavIconType.TIMER, 20));
        logoIcon.putClientProperty("sidebarText", Boolean.TRUE);

        JLabel logoText = new JLabel("Timer Bot");
        logoText.setFont(new Font(FONT_UI, Font.BOLD, 17));
        logoText.putClientProperty("sidebarText", Boolean.TRUE);
        logoText.setBorder(new EmptyBorder(0, 12, 0, 0));

        logoPanel.add(logoIcon);
        logoPanel.add(logoText);
        side.add(logoPanel, BorderLayout.NORTH);

        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setOpaque(false);
        nav.setBorder(new EmptyBorder(4, 12, 4, 12));

        Object[][] items = {
                {NavIconType.HOME,     "Home",           "HOME"},
                {NavIconType.FORGE,    "Live Forges",    "FORGE"},
                {NavIconType.PROFITS,  "Profits",        "PROFITS"},
                {NavIconType.STATS,    "Stat Tracking",  "STATS"},
                {NavIconType.KAT,      "Kat Timers",     "KAT"},
                {NavIconType.CUSTOM,   "Custom Timers",  "CUSTOM"},
                {NavIconType.ACTIVE,   "Active Timers",  "ACTIVE"},
                {NavIconType.SETTINGS, "Settings",       "SETTINGS"}
        };

        for (Object[] item : items) {
            final String key = (String) item[2];
            NavButton nb = new NavButton((NavIconType) item[0], (String) item[1], key, this,
                    () -> showScreen(key));
            nav.add(nb);
            nav.add(Box.createVerticalStrut(4));
        }
        side.add(nav, BorderLayout.CENTER);

        JPanel sig = new JPanel();
        sig.setLayout(new BoxLayout(sig, BoxLayout.Y_AXIS));
        sig.setOpaque(false);
        sig.setBorder(new EmptyBorder(12, 12, 22, 12));

        NavButton helpBtn = new NavButton(NavIconType.HELP, "Help & Contact", "HELP", this,
                () -> showScreen("HELP"));
        helpBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        helpBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        sig.add(helpBtn);
        sig.add(Box.createVerticalStrut(14));

        JPanel sigText = new JPanel();
        sigText.setLayout(new BoxLayout(sigText, BoxLayout.Y_AXIS));
        sigText.setOpaque(false);

        JLabel designedBy = new JLabel("Designed by");
        designedBy.setFont(new Font(FONT_UI, Font.PLAIN, 12));
        designedBy.putClientProperty("sidebarSubtext", Boolean.TRUE);
        designedBy.setAlignmentX(Component.LEFT_ALIGNMENT);

        slivvyLabel = new JLabel("Slivvy");
        slivvyLabel.setFont(new Font(FONT_UI, Font.BOLD | Font.ITALIC, 22));
        slivvyLabel.putClientProperty("rainbow", Boolean.TRUE);
        slivvyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        sigText.add(designedBy);
        sigText.add(Box.createVerticalStrut(2));
        sigText.add(slivvyLabel);
        sigText.setBorder(new EmptyBorder(0, 10, 0, 10));

        sig.add(sigText);
        side.add(sig, BorderLayout.SOUTH);
        return side;
    }

    // ===== HELP SCREEN =====
    private JPanel createHelpScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(32, 36, 24, 36));

        panel.add(buildScreenHeader("Help & Contact", null), BorderLayout.NORTH);

        ScrollablePanel content = new ScrollablePanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JPanel heroCard = new JPanel();
        heroCard.setLayout(new BoxLayout(heroCard, BoxLayout.Y_AXIS));
        heroCard.setOpaque(true);
        heroCard.putClientProperty("card", Boolean.TRUE);
        heroCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        heroCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        heroCard.setPreferredSize(new Dimension(900, 190));
        heroCard.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel headline = new JLabel("Questions, bugs, or suggestions?");
        headline.setFont(new Font(FONT_UI, Font.BOLD, 20));
        headline.putClientProperty("customColor", Boolean.TRUE);
        headline.setForeground(C_GOLD);
        headline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel body1 = new JLabel("Reach out any time \u2014 feature ideas, bug reports,");
        body1.setFont(new Font(FONT_UI, Font.PLAIN, 14));
        body1.putClientProperty("subtext", Boolean.TRUE);
        body1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel body2 = new JLabel("or just want to say hi. I read every message.");
        body2.setFont(new Font(FONT_UI, Font.PLAIN, 14));
        body2.putClientProperty("subtext", Boolean.TRUE);
        body2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel emailLbl = new JLabel("slivvybiz@gmail.com");
        emailLbl.setFont(new Font(FONT_UI, Font.BOLD, 18));
        emailLbl.putClientProperty("customColor", Boolean.TRUE);
        emailLbl.setForeground(C_BLUE);
        emailLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailLbl.setBorder(new EmptyBorder(14, 0, 12, 0));

        FlatButton emailBtn = new FlatButton("Open Mail App",
                FlatButton.Role.ACCENT, this, e -> openMailClient());
        emailBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailBtn.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        emailBtn.setMaximumSize(new Dimension(180, 38));

        heroCard.add(headline);
        heroCard.add(Box.createVerticalStrut(8));
        heroCard.add(body1);
        heroCard.add(body2);
        heroCard.add(emailLbl);
        heroCard.add(emailBtn);

        content.add(heroCard);
        content.add(Box.createVerticalStrut(24));

        JLabel tipsHeading = sectionLabel("Quick Tips");
        tipsHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(tipsHeading);
        content.add(Box.createVerticalStrut(12));

        content.add(helpTipRow("F4", "Toggle the developer console"));
        content.add(helpTipRow("Ctrl+H", "Minimize to the system tray"));
        content.add(helpTipRow("Home", "See active timers, profit, and history"));
        content.add(helpTipRow("Live Forges", "Load your in-game forge slots from the API"));
        content.add(helpTipRow("Profits", "Track Bazaar margins and estimate flips"));
        content.add(helpTipRow("Kat Timers", "Auto-detect upgradable pets on your profile"));
        content.add(helpTipRow("Custom Timers", "Save reusable buttons for anything"));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        installModernScrollbar(scroll);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel helpTipRow(String key, String description) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(true);
        row.putClientProperty("card", Boolean.TRUE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        row.setPreferredSize(new Dimension(900, 48));

        JLabel keyLbl = new JLabel(key);
        keyLbl.setFont(new Font(FONT_UI, Font.BOLD, 13));
        keyLbl.setForeground(C_BLUE);
        keyLbl.setPreferredSize(new Dimension(130, 24));

        JLabel descLbl = new JLabel(description);
        descLbl.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        descLbl.putClientProperty("subtext", Boolean.TRUE);

        row.add(keyLbl, BorderLayout.WEST);
        row.add(descLbl, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        wrapper.add(row, BorderLayout.CENTER);
        wrapper.setBorder(new EmptyBorder(0, 0, 8, 0));
        return wrapper;
    }

    private void openMailClient() {
        try {
            if (Desktop.isDesktopSupported() &&
                    Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
                Desktop.getDesktop().mail(new URI("mailto:slivvybiz@gmail.com"));
            } else {
                setStatus("Could not open mail app \u2014 email slivvybiz@gmail.com");
            }
        } catch (Exception ex) {
            setStatus("Could not open mail app \u2014 email slivvybiz@gmail.com");
        }
    }

    private void showScreen(String key) {
        currentScreen = key;
        cardLayout.show(screens, key);
        for (NavButton nb : navButtons) nb.setNavSelected(nb.key.equals(key));
        if ("CUSTOM".equals(key)) refreshCustomButtonsGrid();
        if ("HOME".equals(key)) {
            statsDirty = true;
            refreshStatsUI();
        }
        if ("PROFITS".equals(key)) refreshProfitsUI();
    }

    // ===== MINIMIZE TO TRAY =====
    private void minimizeToTray() {
        if (!trayAvailable || trayIcon == null) {
            setExtendedState(JFrame.ICONIFIED);
            setStatus("System tray unavailable \u2014 minimized to taskbar.");
            return;
        }
        setVisible(false);
        showDesktopNotification("Timer Bot is still running",
                "Timers continue in the background. Click the tray icon to restore.");
        setStatus("Minimized to system tray.");
    }

    // ===== HOME SCREEN =====
    private JPanel createHomeScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(28, 36, 24, 36));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        MinecraftTitleLabel title = new MinecraftTitleLabel("HYPIXEL TIMERS", 24, true);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Track forges, Kat upgrades, and custom grinds \u2014 all in one place.");
        subtitle.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        subtitle.putClientProperty("subtext", Boolean.TRUE);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(10, 4, 0, 0));

        header.add(title);
        header.add(subtitle);
        header.add(Box.createVerticalStrut(20));
        panel.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        JPanel heroRow = new JPanel(new GridLayout(1, 2, 16, 0));
        heroRow.setOpaque(false);
        heroRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        heroRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        heroRow.setPreferredSize(new Dimension(900, 132));

        HeroCard profitCard = new HeroCard(
                "TOTAL PROFIT", "0 coins", "Complete a forge timer to track",
                new Color(70, 200, 120), HypixelTimerApp.this);
        heroProfitValue = profitCard.valueLabel;
        heroProfitSub   = profitCard.subLabel;

        HeroCard bestFlipCard = new HeroCard(
                "BEST FLIP RIGHT NOW", "\u2014", "Waiting on Bazaar prices",
                new Color(240, 180, 60), HypixelTimerApp.this);
        heroBestFlipName = bestFlipCard.valueLabel;
        heroBestFlipSub  = bestFlipCard.subLabel;

        heroRow.add(profitCard);
        heroRow.add(bestFlipCard);
        body.add(heroRow);
        body.add(Box.createVerticalStrut(16));

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK),
                        "minimizeToTrayShortcut");
        getRootPane().getActionMap().put("minimizeToTrayShortcut", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                minimizeToTray();
            }
        });

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 14, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        StatCard activeCard    = new StatCard("Active timers", "0", HypixelTimerApp.this);
        StatCard completedCard = new StatCard("Completed",     "0", HypixelTimerApp.this);
        StatCard timeCard      = new StatCard("Time tracked",  "0s", HypixelTimerApp.this);

        statActiveValue    = activeCard.valueLabel;
        statCompletedValue = completedCard.valueLabel;
        statTimeValue      = timeCard.valueLabel;

        statsRow.add(activeCard);
        statsRow.add(completedCard);
        statsRow.add(timeCard);
        body.add(statsRow);
        body.add(Box.createVerticalStrut(20));
        body.add(buildNextUpBox());
        body.add(Box.createVerticalStrut(14));

        JPanel trayRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        trayRow.setOpaque(false);
        trayRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        trayRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        FlatButton minimizeBtn = new FlatButton("\u25BC  Minimize to Tray",
                FlatButton.Role.DEFAULT, this, e -> minimizeToTray());
        minimizeBtn.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        minimizeBtn.setPreferredSize(new Dimension(180, 34));
        trayRow.add(minimizeBtn);
        body.add(trayRow);

        body.add(Box.createVerticalStrut(20));

        body.add(sectionLabel("Timer History"));
        body.add(Box.createVerticalStrut(10));

        historyListContainer = new JPanel();
        historyListContainer.setLayout(new BoxLayout(historyListContainer, BoxLayout.Y_AXIS));
        historyListContainer.setOpaque(false);
        JScrollPane scroll = new JScrollPane(historyListContainer);
        scroll.setBorder(BorderFactory.createLineBorder(cBorder, 1));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        installModernScrollbar(scroll);
        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.setOpaque(false);
        scrollWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollWrapper.add(scroll, BorderLayout.CENTER);
        body.add(scrollWrapper);

        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildNextUpBox() {
        nextUpBox = new JPanel(new BorderLayout());
        nextUpBox.setOpaque(true);
        nextUpBox.putClientProperty("nextUpBox", Boolean.TRUE);
        nextUpBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        nextUpBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        nextUpBox.setPreferredSize(new Dimension(600, 84));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel nextUpHeader = new JLabel("NEXT UP");
        nextUpHeader.setFont(new Font(FONT_UI, Font.BOLD, 10));
        nextUpHeader.putClientProperty("nextUpSubtext", Boolean.TRUE);
        nextUpHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        nextUpTitleLabel = new JLabel("No timers running");
        nextUpTitleLabel.setFont(new Font(FONT_UI, Font.BOLD, 16));
        nextUpTitleLabel.putClientProperty("nextUpTitle", Boolean.TRUE);
        nextUpTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(nextUpHeader);
        left.add(Box.createVerticalStrut(4));
        left.add(nextUpTitleLabel);

        nextUpTimeLabel = new JLabel("");
        nextUpTimeLabel.setFont(new Font(FONT_UI, Font.BOLD, 22));
        nextUpTimeLabel.putClientProperty("nextUpTime", Boolean.TRUE);
        nextUpTimeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        nextUpBox.add(left, BorderLayout.WEST);
        nextUpBox.add(nextUpTimeLabel, BorderLayout.EAST);

        return nextUpBox;
    }

    private void refreshStatsUI() {
        if (statActiveValue != null) statActiveValue.setText(String.valueOf(activeTimers.size()));

        int totalCompleted = 0;
        synchronized (statsLock) {
            for (TimerStats s : timerStats.values()) totalCompleted += s.completedCount;
        }
        if (statCompletedValue != null) statCompletedValue.setText(String.valueOf(totalCompleted));
        if (statTimeValue != null) statTimeValue.setText(formatDuration(getTotalActivityMillis()));

        if (heroProfitValue != null) {
            heroProfitValue.setText(formatCoins(totalProfitCoins) + " coins");
        }
        if (heroProfitSub != null) {
            int recipes = perRecipeProfit.size();
            if (totalProfitCoins > 0 && recipes > 0) {
                heroProfitSub.setText("Across " + recipes + " recipe" + (recipes == 1 ? "" : "s"));
            } else {
                heroProfitSub.setText("Complete a forge timer to track");
            }
        }

        if (heroBestFlipName != null && heroBestFlipSub != null) {
            if (bazaarBuyPrices.isEmpty()) {
                heroBestFlipName.setText("\u2014");
                heroBestFlipSub.setText("Waiting on Bazaar prices");
            } else {
                String bestKey = null;
                double bestMargin = 0;
                for (String key : FORGE_RECIPES.keySet()) {
                    double m = computeRecipeProfitPerHour(key);
                    if (m > bestMargin) { bestMargin = m; bestKey = key; }
                }
                if (bestKey == null || bestMargin <= 0) {
                    heroBestFlipName.setText("No profitable flip");
                    heroBestFlipSub.setText("Prices are unfavorable right now");
                } else {
                    heroBestFlipName.setText(titleCase(bestKey.toLowerCase()));
                    heroBestFlipSub.setText("+" + formatCoins(bestMargin) + " coins / hr");
                }
            }
        }

        refreshNextUp();

        if (statsDirty && historyListContainer != null) {
            rebuildHistoryList();
            statsDirty = false;
        }
    }

    private void refreshNextUp() {
        if (nextUpTitleLabel == null || nextUpTimeLabel == null) return;

        if (activeTimers.isEmpty()) {
            nextUpTitleLabel.setText("No timers running");
            nextUpTimeLabel.setText("");
            return;
        }
        ActiveTimer soonest = null;
        for (ActiveTimer t : activeTimers) {
            if (soonest == null || t.endTimeMillis < soonest.endTimeMillis) soonest = t;
        }
        long remaining = soonest.endTimeMillis - System.currentTimeMillis();
        nextUpTitleLabel.setText(soonest.label);
        nextUpTimeLabel.setText(formatDuration(remaining));
    }

    private void rebuildHistoryList() {
        if (historyListContainer == null) return;
        historyListContainer.removeAll();

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setBorder(new EmptyBorder(8, 14, 8, 14));
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel hName = new JLabel("Timer");
        JLabel hStat = new JLabel("Started / Completed");
        hName.setFont(new Font(FONT_UI, Font.BOLD, 12));
        hStat.setFont(new Font(FONT_UI, Font.BOLD, 12));
        hName.putClientProperty("nextUpSubtext", Boolean.TRUE);
        hStat.putClientProperty("nextUpSubtext", Boolean.TRUE);

        headerRow.add(hName, BorderLayout.WEST);
        headerRow.add(hStat, BorderLayout.EAST);
        historyListContainer.add(headerRow);

        JSeparator sep = new JSeparator();
        sep.setForeground(cBorder);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        historyListContainer.add(sep);

        List<Map.Entry<String, TimerStats>> entries;
        synchronized (statsLock) {
            entries = new ArrayList<>(timerStats.entrySet());
        }

        if (entries.isEmpty()) {
            JLabel empty = new JLabel("No timers started yet.");
            empty.setFont(new Font(FONT_UI, Font.PLAIN, 13));
            empty.putClientProperty("subtext", Boolean.TRUE);
            empty.setBorder(new EmptyBorder(12, 14, 12, 14));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            historyListContainer.add(empty);
        } else {
            for (Map.Entry<String, TimerStats> e : entries) {
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);
                row.setBorder(new EmptyBorder(8, 14, 8, 14));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

                JLabel name = new JLabel(e.getKey());
                name.setFont(new Font(FONT_UI, Font.PLAIN, 13));

                JLabel counts = new JLabel(e.getValue().startedCount + "  /  " + e.getValue().completedCount);
                counts.setFont(new Font(FONT_UI, Font.PLAIN, 13));
                counts.putClientProperty("subtext", Boolean.TRUE);

                row.add(name, BorderLayout.WEST);
                row.add(counts, BorderLayout.EAST);
                historyListContainer.add(row);
            }
        }

        applyThemeToTree(historyListContainer);
        historyListContainer.revalidate();
        historyListContainer.repaint();
    }

    private long getTotalActivityMillis() {
        synchronized (statsLock) {
            if (activityStartMillis > 0) {
                return accumulatedActivityMillis + (System.currentTimeMillis() - activityStartMillis);
            }
            return accumulatedActivityMillis;
        }
    }

    // ===== LIVE FORGES SECTION =====
    private JPanel buildLiveForgesSection() {
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel title = new JLabel("Your Active Forges");
        title.setFont(new Font(FONT_UI, Font.BOLD, 16));
        title.setForeground(cText);

        JLabel hint = new JLabel("Live from api.hypixel.net");
        hint.setFont(new Font(FONT_UI, Font.PLAIN, 11));
        hint.putClientProperty("subtext", Boolean.TRUE);

        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(hint, BorderLayout.EAST);
        wrap.add(headerRow);
        wrap.add(Box.createVerticalStrut(10));

        JPanel inputCard = new JPanel(new BorderLayout(12, 0));
        inputCard.setOpaque(true);
        inputCard.putClientProperty("card", Boolean.TRUE);
        inputCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        inputCard.setPreferredSize(new Dimension(600, 72));

        JLabel userLbl = smallLabel("Username:");
        forgeUsernameField = new JTextField(16);
        forgeUsernameField.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        if (lastUsername != null && !lastUsername.isEmpty()) {
            forgeUsernameField.setText(lastUsername);
        }
        styleInput(forgeUsernameField);
        forgeUsernameField.addActionListener(e -> onFetchForgesClicked());

        forgeRefreshBtn = new FlatButton("Load Forges", FlatButton.Role.ACCENT, this,
                e -> onFetchForgesClicked());

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftGroup.setOpaque(false);
        leftGroup.add(userLbl);
        leftGroup.add(forgeUsernameField);

        inputCard.add(leftGroup, BorderLayout.CENTER);
        inputCard.add(forgeRefreshBtn, BorderLayout.EAST);
        wrap.add(inputCard);
        wrap.add(Box.createVerticalStrut(12));

        forgeStatusLabel = new JLabel("Enter a username to load your active forges.");
        forgeStatusLabel.setFont(new Font(FONT_UI, Font.PLAIN, 12));
        forgeStatusLabel.putClientProperty("subtext", Boolean.TRUE);
        forgeStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        forgeStatusLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        wrap.add(forgeStatusLabel);
        wrap.add(Box.createVerticalStrut(10));

        activeForgesContainer = new JPanel();
        activeForgesContainer.setLayout(new BoxLayout(activeForgesContainer, BoxLayout.Y_AXIS));
        activeForgesContainer.setOpaque(false);
        activeForgesContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(activeForgesContainer);

        return wrap;
    }

    private void onFetchForgesClicked() {
        if (!ensureApiKey()) return;
        String username = forgeUsernameField.getText().trim();
        if (username.isEmpty()) {
            warn("Please enter a Minecraft username.");
            return;
        }

        lastUsername = username;
        prefs.put("lastUsername", username);

        forgeRefreshBtn.setEnabled(false);
        forgeRows.clear();
        activeForgesContainer.removeAll();

        JLabel loading = new JLabel("Fetching active forges for " + username + "...");
        loading.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        loading.setForeground(cText);
        loading.setAlignmentX(Component.LEFT_ALIGNMENT);
        activeForgesContainer.add(loading);
        activeForgesContainer.revalidate();
        activeForgesContainer.repaint();

        forgeStatusLabel.setText("Contacting Hypixel API...");

        final String userFinal = username;

        fetchUuid(userFinal)
                .thenCompose(uuid ->
                        fetchSkyblockProfiles(uuid, DEVELOPER_API_KEY)
                                .thenApply(sb -> {
                                    ForgeFetchResult r = new ForgeFetchResult();
                                    r.username = userFinal;
                                    r.uuid = uuid;
                                    r.skyblockData = sb;
                                    return r;
                                }))
                .thenAccept(this::displayActiveForges)
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    SwingUtilities.invokeLater(() -> {
                        forgeStatusLabel.setText("Error: " + cause.getMessage());
                        activeForgesContainer.removeAll();
                        JLabel err = new JLabel("<html><body style='width:520px'>Could not load forges.<br><br>"
                                + "  &bull; Check the username is correct<br>"
                                + "  &bull; Make sure the player has a SkyBlock profile<br>"
                                + "  &bull; API key may be rate-limited</body></html>");
                        err.setFont(new Font(FONT_UI, Font.PLAIN, 12));
                        err.putClientProperty("subtext", Boolean.TRUE);
                        err.setAlignmentX(Component.LEFT_ALIGNMENT);
                        activeForgesContainer.add(err);
                        applyThemeToTree(activeForgesContainer);
                        activeForgesContainer.revalidate();
                        activeForgesContainer.repaint();
                        forgeRefreshBtn.setEnabled(true);
                    });
                    return null;
                });
    }

    private static class ForgeFetchResult {
        String username;
        String uuid;
        Object skyblockData;
    }

    @SuppressWarnings("unchecked")
    private void displayActiveForges(ForgeFetchResult r) {
        SwingUtilities.invokeLater(() -> {
            forgeRows.clear();
            activeForgesContainer.removeAll();

            Object profiles = Json.getPath(r.skyblockData, "profiles");
            Object activeProfile = null;
            String cuteName = "Unknown";
            if (profiles instanceof List) {
                List<?> list = (List<?>) profiles;
                for (Object p : list) {
                    if (Boolean.TRUE.equals(Json.getPath(p, "selected"))) {
                        activeProfile = p;
                        break;
                    }
                }
                if (activeProfile == null && !list.isEmpty()) activeProfile = list.get(0);
                if (activeProfile != null) {
                    String cn = Json.str(Json.getPath(activeProfile, "cute_name"));
                    if (cn != null) cuteName = cn;
                }
            }

            if (activeProfile == null) {
                forgeStatusLabel.setText("No SkyBlock profile found for " + r.username + ".");
                forgeRefreshBtn.setEnabled(true);
                return;
            }

            Object members = Json.getPath(activeProfile, "members");
            Object member = (members instanceof Map) ? ((Map<?, ?>) members).get(r.uuid) : null;

            if (member == null) {
                forgeStatusLabel.setText("Player is not a member of the active profile.");
                forgeRefreshBtn.setEnabled(true);
                return;
            }

            List<ActiveForge> forges = extractActiveForges(member);
            if (forges.isEmpty()) {
                forgeStatusLabel.setText("Profile " + cuteName + " \u2014 no active forges right now.");
                JLabel empty = new JLabel("The forge is empty. Start something in-game, then hit Load Forges.");
                empty.setFont(new Font(FONT_UI, Font.PLAIN, 13));
                empty.putClientProperty("subtext", Boolean.TRUE);
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                empty.setBorder(new EmptyBorder(8, 4, 4, 4));
                activeForgesContainer.add(empty);
            } else {
                forgeStatusLabel.setText("Profile " + cuteName + " \u2014 " + forges.size()
                        + " active forge" + (forges.size() == 1 ? "" : "s"));
                for (ActiveForge f : forges) {
                    activeForgesContainer.add(buildForgeRow(f));
                    activeForgesContainer.add(Box.createVerticalStrut(8));
                }
            }

            applyThemeToTree(activeForgesContainer);
            activeForgesContainer.revalidate();
            activeForgesContainer.repaint();
            forgeRefreshBtn.setEnabled(true);
        });
    }

    @SuppressWarnings("unchecked")
    private List<ActiveForge> extractActiveForges(Object member) {
        List<ActiveForge> out = new ArrayList<>();
        Object forge = Json.getPath(member, "forge");
        if (forge == null) return out;

        Object processes = Json.getPath(forge, "forge_processes");
        if (processes == null) return out;

        Map<String, Object> slots = new LinkedHashMap<>();
        collectForgeSlots(processes, slots);

        for (Object slot : slots.values()) {
            if (!(slot instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) slot;

            String type = Json.str(m.get("id"));
            if (type == null || type.isEmpty()) continue;

            long startTime = Json.lng(m.get("startTime"));
            if (startTime <= 0) continue;

            Long dur = FORGE_DURATIONS.get(type);
            if (dur == null) continue;

            ActiveForge f = new ActiveForge();
            f.type = type;
            f.displayName = titleCase(type.toLowerCase());
            f.startTime = startTime;
            f.endTime = startTime + dur;
            out.add(f);
        }

        out.sort((a, b) -> Long.compare(a.endTime, b.endTime));
        return out;
    }

    @SuppressWarnings("unchecked")
    private void collectForgeSlots(Object obj, Map<String, Object> out) {
        if (!(obj instanceof Map)) return;
        Map<String, Object> m = (Map<String, Object>) obj;

        if (m.containsKey("type") && m.containsKey("startTime")) {
            String id = Json.str(m.get("id"));
            long startTime = Json.lng(m.get("startTime"));
            String key = (id != null ? id : "slot") + "|" + startTime;
            out.put(key, m);
            return;
        }
        for (Map.Entry<String, Object> e : m.entrySet()) {
            collectForgeSlots(e.getValue(), out);
        }
    }

    private JPanel buildForgeRow(ActiveForge forge) {
        long now = System.currentTimeMillis();

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(true);
        row.putClientProperty("card", Boolean.TRUE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel nameLbl = new JLabel(forge.displayName);
        nameLbl.setFont(new Font(FONT_UI, Font.BOLD, 15));
        nameLbl.setForeground(cText);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel infoLbl = new JLabel();
        infoLbl.setFont(new Font(FONT_UI, Font.PLAIN, 12));
        infoLbl.setForeground(cSubtext);
        infoLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel badgeLbl = new JLabel(" ");
        badgeLbl.setFont(new Font(FONT_UI, Font.BOLD, 11));
        badgeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(nameLbl);
        left.add(Box.createVerticalStrut(3));
        left.add(infoLbl);
        left.add(Box.createVerticalStrut(3));
        left.add(badgeLbl);

        row.add(left, BorderLayout.CENTER);

        FlatButton remind = new FlatButton("Remind me", FlatButton.Role.SUCCESS, this, e -> {
            long rem = forge.endTime - System.currentTimeMillis();
            if (rem <= 0) {
                setStatus("That forge is already complete \u2014 claim it in-game.");
                return;
            }
            startPresetTimer("Forge: " + forge.displayName,
                    forge.displayName + " is done forging!",
                    rem, TimeUnit.MILLISECONDS);
            setStatus("Reminder set \u2014 " + forge.displayName + " (" + formatDuration(rem) + ")");
        });
        remind.setPreferredSize(new Dimension(112, 40));
        row.add(remind, BorderLayout.EAST);

        forgeRows.add(new ForgeRowRefs(forge.startTime, forge.endTime, infoLbl, badgeLbl));
        updateForgeRowLabels(forgeRows.get(forgeRows.size() - 1), now);

        return row;
    }

    private void updateForgeRowLabels(ForgeRowRefs refs, long now) {
        long remaining = refs.endTime - now;
        String started = formatDate(refs.startTime);

        if (remaining <= 0) {
            refs.infoLabel.setText("Started " + started);
            refs.badgeLabel.setText("\u25CF  READY TO CLAIM");
            refs.badgeLabel.setForeground(new Color(70, 200, 100));
        } else {
            refs.infoLabel.setText("Done in " + formatDuration(remaining) + "  \u2022  started " + started);
            if (remaining < TimeUnit.MINUTES.toMillis(5)) {
                refs.badgeLabel.setText("\u25CF  ALMOST DONE");
                refs.badgeLabel.setForeground(new Color(240, 180, 60));
            } else {
                refs.badgeLabel.setText(" ");
            }
        }
    }

    private void tickForgeRows() {
        long now = System.currentTimeMillis();
        for (ForgeRowRefs r : forgeRows) {
            updateForgeRowLabels(r, now);
        }
    }

    private JPanel createForgeTrackerScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(32, 36, 24, 36));

        panel.add(buildScreenHeader("Live Forges",
                "Pulls your active forges straight from the Hypixel SkyBlock API."),
                BorderLayout.NORTH);

        ScrollablePanel content = new ScrollablePanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JPanel liveCard = buildLiveForgesSection();
        liveCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(liveCard);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        installModernScrollbar(scroll);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ===== KAT TIMERS SCREEN =====
    private JPanel createKatScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(32, 36, 24, 36));

        panel.add(buildScreenHeader("Kat Timers",
                "Auto-loads your upgradable pets from the Hypixel API. Manual starters below."),
                BorderLayout.NORTH);

        ScrollablePanel content = new ScrollablePanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        content.add(buildKatAutoSection());
        content.add(Box.createVerticalStrut(28));

        JPanel manualHeader = new JPanel(new BorderLayout(8, 0));
        manualHeader.setOpaque(false);
        manualHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        manualHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JPanel manualBar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(160, 160, 175));
                g2.fillRoundRect(0, 2, 4, Math.max(4, getHeight() - 4), 3, 3);
                g2.dispose();
            }
        };
        manualBar.setOpaque(false);
        manualBar.setPreferredSize(new Dimension(6, 22));

        JLabel manualTitle = new JLabel("Manual Kat Starters");
        manualTitle.setFont(new Font(FONT_UI, Font.BOLD, 15));
        manualTitle.setForeground(cSubtext);

        manualHeader.add(manualBar, BorderLayout.WEST);
        manualHeader.add(manualTitle, BorderLayout.CENTER);
        content.add(manualHeader);
        content.add(Box.createVerticalStrut(12));

        for (Map.Entry<String, List<KatRecipe>> e : KAT_RECIPES.entrySet()) {
            content.add(katPetSection(e.getKey(), e.getValue()));
            content.add(Box.createVerticalStrut(18));
        }

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getVerticalScrollBar().setBlockIncrement(100);
        installModernScrollbar(scroll);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildKatAutoSection() {
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel title = new JLabel("Your Upgradable Pets");
        title.setFont(new Font(FONT_UI, Font.BOLD, 16));
        title.setForeground(cText);

        JLabel hint = new JLabel("Live from api.hypixel.net");
        hint.setFont(new Font(FONT_UI, Font.PLAIN, 11));
        hint.putClientProperty("subtext", Boolean.TRUE);

        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(hint, BorderLayout.EAST);
        wrap.add(headerRow);
        wrap.add(Box.createVerticalStrut(10));

        JPanel inputCard = new JPanel(new BorderLayout(12, 0));
        inputCard.setOpaque(true);
        inputCard.putClientProperty("card", Boolean.TRUE);
        inputCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        inputCard.setPreferredSize(new Dimension(600, 72));

        JLabel userLbl = smallLabel("Username:");
        katUsernameField = new JTextField(16);
        katUsernameField.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        if (lastUsername != null && !lastUsername.isEmpty()) {
            katUsernameField.setText(lastUsername);
        }
        styleInput(katUsernameField);
        katUsernameField.addActionListener(e -> onFetchKatClicked());

        katRefreshBtn = new FlatButton("Load Pets", FlatButton.Role.SUCCESS, this,
                e -> onFetchKatClicked());

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftGroup.setOpaque(false);
        leftGroup.add(userLbl);
        leftGroup.add(katUsernameField);

        inputCard.add(leftGroup, BorderLayout.CENTER);
        inputCard.add(katRefreshBtn, BorderLayout.EAST);
        wrap.add(inputCard);
        wrap.add(Box.createVerticalStrut(12));

        katStatusLabel = new JLabel("Enter a username to load upgradable pets.");
        katStatusLabel.setFont(new Font(FONT_UI, Font.PLAIN, 12));
        katStatusLabel.putClientProperty("subtext", Boolean.TRUE);
        katStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        katStatusLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        wrap.add(katStatusLabel);
        wrap.add(Box.createVerticalStrut(10));

        activeKatContainer = new JPanel();
        activeKatContainer.setLayout(new BoxLayout(activeKatContainer, BoxLayout.Y_AXIS));
        activeKatContainer.setOpaque(false);
        activeKatContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(activeKatContainer);

        return wrap;
    }

    private void onFetchKatClicked() {
        if (!ensureApiKey()) return;
        String username = katUsernameField.getText().trim();
        if (username.isEmpty()) {
            warn("Please enter a Minecraft username.");
            return;
        }

        lastUsername = username;
        prefs.put("lastUsername", username);

        katRefreshBtn.setEnabled(false);
        activeKatContainer.removeAll();

        JLabel loading = new JLabel("Fetching pets for " + username + "...");
        loading.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        loading.setForeground(cText);
        loading.setAlignmentX(Component.LEFT_ALIGNMENT);
        activeKatContainer.add(loading);
        activeKatContainer.revalidate();
        activeKatContainer.repaint();

        katStatusLabel.setText("Contacting Hypixel API...");

        final String userFinal = username;

        fetchUuid(userFinal)
                .thenCompose(uuid ->
                        fetchSkyblockProfiles(uuid, DEVELOPER_API_KEY)
                                .thenApply(sb -> {
                                    ForgeFetchResult r = new ForgeFetchResult();
                                    r.username = userFinal;
                                    r.uuid = uuid;
                                    r.skyblockData = sb;
                                    return r;
                                }))
                .thenAccept(this::displayUpgradablePets)
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    final String msg = cause.getMessage() != null
                            ? cause.getMessage()
                            : cause.toString();

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            katStatusLabel.setText("Error: " + msg);
                            activeKatContainer.removeAll();
                            JLabel err = new JLabel("<html><body style='width:520px'>"
                                    + "Could not load pets.<br><br>"
                                    + "  &bull; Check the username is correct<br>"
                                    + "  &bull; Make sure the player has a SkyBlock profile<br>"
                                    + "  &bull; API key may be rate-limited</body></html>");
                            err.setFont(new Font(FONT_UI, Font.PLAIN, 12));
                            err.putClientProperty("subtext", Boolean.TRUE);
                            err.setAlignmentX(Component.LEFT_ALIGNMENT);
                            activeKatContainer.add(err);
                            applyThemeToTree(activeKatContainer);
                            activeKatContainer.revalidate();
                            activeKatContainer.repaint();
                            katRefreshBtn.setEnabled(true);
                        }
                    });
                    return null;
                });
    }

    @SuppressWarnings("unchecked")
    private void displayUpgradablePets(final ForgeFetchResult r) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                activeKatContainer.removeAll();

                Object profiles = Json.getPath(r.skyblockData, "profiles");
                Object activeProfile = null;
                String cuteName = "Unknown";
                if (profiles instanceof List) {
                    List<?> list = (List<?>) profiles;
                    for (Object p : list) {
                        if (Boolean.TRUE.equals(Json.getPath(p, "selected"))) {
                            activeProfile = p;
                            break;
                        }
                    }
                    if (activeProfile == null && !list.isEmpty()) activeProfile = list.get(0);
                    if (activeProfile != null) {
                        String cn = Json.str(Json.getPath(activeProfile, "cute_name"));
                        if (cn != null) cuteName = cn;
                    }
                }

                if (activeProfile == null) {
                    katStatusLabel.setText("No SkyBlock profile found for " + r.username + ".");
                    katRefreshBtn.setEnabled(true);
                    return;
                }

                Object members = Json.getPath(activeProfile, "members");
                Object member = (members instanceof Map) ? ((Map<?, ?>) members).get(r.uuid) : null;

                if (member == null) {
                    katStatusLabel.setText("Player is not a member of the active profile.");
                    katRefreshBtn.setEnabled(true);
                    return;
                }

                Object petsArr = Json.getPath(member, "pets_data", "pets");
                if (!(petsArr instanceof List)) {
                    katStatusLabel.setText("Profile " + cuteName + " has no pet data.");
                    JLabel empty = new JLabel("No pets found on this profile.");
                    empty.setFont(new Font(FONT_UI, Font.PLAIN, 13));
                    empty.putClientProperty("subtext", Boolean.TRUE);
                    empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                    empty.setBorder(new EmptyBorder(8, 4, 4, 4));
                    activeKatContainer.add(empty);
                    applyThemeToTree(activeKatContainer);
                    activeKatContainer.revalidate();
                    activeKatContainer.repaint();
                    katRefreshBtn.setEnabled(true);
                    return;
                }

                Map<String, String> bestPetPerType = new LinkedHashMap<>();
                Map<String, Integer> bestTierIdx = new LinkedHashMap<>();

                for (Object petObj : (List<?>) petsArr) {
                    if (!(petObj instanceof Map)) continue;
                    String type = Json.str(Json.getPath(petObj, "type"));
                    String tier = Json.str(Json.getPath(petObj, "tier"));
                    if (type == null || tier == null) continue;

                    int idx = katTierIndex(tier);
                    if (idx < 0) continue;

                    Integer cur = bestTierIdx.get(type);
                    if (cur == null || idx > cur) {
                        bestTierIdx.put(type, idx);
                        bestPetPerType.put(type, tier);
                    }
                }

                List<Map.Entry<String, String>> sorted = new ArrayList<>(bestPetPerType.entrySet());
                sorted.sort(Map.Entry.comparingByKey());

                int upgradeable = 0;
                int unknown = 0;

                for (Map.Entry<String, String> e : sorted) {
                    String type = e.getKey();
                    String tier = e.getValue();
                    int tierIdx = bestTierIdx.get(type);

                    List<KatRecipe> recipes = KAT_RECIPES.get(type);
                    if (recipes == null) {
                        unknown++;
                        continue;
                    }
                    if (tierIdx < 0 || tierIdx >= recipes.size()) continue;

                    KatRecipe recipe = recipes.get(tierIdx);
                    String nextTier = KAT_RARITIES[tierIdx + 1];
                    activeKatContainer.add(buildKatUpgradeRow(type, tier, nextTier, recipe));
                    activeKatContainer.add(Box.createVerticalStrut(10));
                    upgradeable++;
                }

                if (upgradeable == 0) {
                    katStatusLabel.setText("Profile " + cuteName + " \u2014 no upgradable pets found.");
                    JLabel empty = new JLabel("No upgradable pets found (either maxed or not in our catalog).");
                    empty.setFont(new Font(FONT_UI, Font.PLAIN, 13));
                    empty.putClientProperty("subtext", Boolean.TRUE);
                    empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                    empty.setBorder(new EmptyBorder(8, 4, 4, 4));
                    activeKatContainer.add(empty);
                } else {
                    String msg = "Profile " + cuteName + " \u2014 " + upgradeable + " pet"
                            + (upgradeable == 1 ? "" : "s") + " upgradable";
                    if (unknown > 0) msg += "  (" + unknown + " not in catalog)";
                    katStatusLabel.setText(msg);
                }

                applyThemeToTree(activeKatContainer);
                activeKatContainer.revalidate();
                activeKatContainer.repaint();
                katRefreshBtn.setEnabled(true);
            }
        });
    }

    private JPanel buildKatUpgradeRow(String petType, String fromTier, String toTier, KatRecipe recipe) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(true);
        row.putClientProperty("card", Boolean.TRUE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        row.setPreferredSize(new Dimension(900, 130));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel nameLbl = new JLabel(titleCase(petType.toLowerCase()));
        nameLbl.setFont(new Font(FONT_UI, Font.BOLD, 15));
        nameLbl.setForeground(cText);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel rarityLbl = new JLabel(titleCase(fromTier.toLowerCase()) + "  \u2192  " + titleCase(toTier.toLowerCase()));
        rarityLbl.setFont(new Font(FONT_UI, Font.PLAIN, 12));
        rarityLbl.putClientProperty("subtext", Boolean.TRUE);
        rarityLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        String costStr = formatCoins(recipe.coinCost) + " coins   \u2022   " + formatDuration(recipe.durationMillis);
        JLabel costLbl = new JLabel(costStr);
        costLbl.setFont(new Font(FONT_UI, Font.BOLD, 12));
        costLbl.setForeground(C_GOLD);
        costLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        String matStr = recipe.materials.length == 0
                ? "No extra materials"
                : String.join("  \u2022  ", recipe.materials);
        JLabel matLbl = new JLabel("<html><body style='width:640px'>" + matStr + "</body></html>");
        matLbl.setFont(new Font(FONT_UI, Font.PLAIN, 11));
        matLbl.putClientProperty("subtext", Boolean.TRUE);
        matLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(nameLbl);
        left.add(Box.createVerticalStrut(4));
        left.add(rarityLbl);
        left.add(Box.createVerticalStrut(4));
        left.add(costLbl);
        left.add(Box.createVerticalStrut(6));
        left.add(matLbl);

        row.add(left, BorderLayout.CENTER);

        final String fullLabel = titleCase(petType.toLowerCase()) + ": "
                + titleCase(fromTier.toLowerCase()) + " \u2192 " + titleCase(toTier.toLowerCase());
        FlatButton start = new FlatButton("Start timer", FlatButton.Role.SUCCESS, this, e -> {
            startPresetTimer(fullLabel, titleCase(petType.toLowerCase()) + " is ready to claim!",
                    recipe.durationMillis, TimeUnit.MILLISECONDS);
            setStatus("Started Kat upgrade: " + fullLabel);
        });
        start.setPreferredSize(new Dimension(120, 40));
        row.add(start, BorderLayout.EAST);

        return row;
    }

    private JPanel katPetSection(String petType, List<KatRecipe> recipes) {
        final int BTN_HEIGHT = 74;

        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = sectionLabel(titleCase(petType.toLowerCase()));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(title);
        section.add(Box.createVerticalStrut(8));

        int count = recipes.size();
        JPanel row = new JPanel(new GridLayout(1, count, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(700, BTN_HEIGHT));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, BTN_HEIGHT));

        for (int i = 0; i < count; i++) {
            String fromRarity = KAT_RARITIES[i];
            String toRarity = KAT_RARITIES[i + 1];
            KatRecipe recipe = recipes.get(i);

            final String fullLabel = titleCase(petType.toLowerCase()) + ": "
                    + titleCase(fromRarity.toLowerCase()) + " \u2192 " + titleCase(toRarity.toLowerCase());
            final long dur = recipe.durationMillis;
            final String petTitle = titleCase(petType.toLowerCase());

            FlatButton b = new FlatButton(
                    "<html><center>" + titleCase(fromRarity.toLowerCase()) + " \u2192 " + titleCase(toRarity.toLowerCase())
                            + "<br><span style='font-size:10px'>" + formatDuration(dur) + "</span></center></html>",
                    FlatButton.Role.DEFAULT, this,
                    e -> {
                        startPresetTimer(fullLabel, petTitle + " is ready to claim!",
                                dur, TimeUnit.MILLISECONDS);
                        setStatus("Started: " + fullLabel);
                    });
            b.setFont(new Font(FONT_UI, Font.PLAIN, 12));
            b.setPreferredSize(new Dimension(140, BTN_HEIGHT));
            b.setMinimumSize(new Dimension(90, BTN_HEIGHT));
            row.add(b);
        }

        section.add(row);
        return section;
    }

    // ===== CUSTOM TIMERS SCREEN =====
    private JPanel createCustomScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(32, 36, 24, 36));

        panel.add(buildScreenHeader("Custom Timers",
                "Save reusable buttons for anything you time regularly."), BorderLayout.NORTH);

        ScrollablePanel content = new ScrollablePanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JPanel addForm = buildAddForm();
        addForm.setAlignmentX(Component.LEFT_ALIGNMENT);
        addForm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        addForm.setPreferredSize(new Dimension(700, 76));
        content.add(addForm);
        content.add(Box.createVerticalStrut(24));

        JLabel lbl = sectionLabel("Your Custom Buttons");
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(lbl);
        content.add(Box.createVerticalStrut(10));

        customButtonsContainer = new JPanel();
        customButtonsContainer.setLayout(new BoxLayout(customButtonsContainer, BoxLayout.Y_AXIS));
        customButtonsContainer.setOpaque(false);
        customButtonsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(customButtonsContainer);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        installModernScrollbar(scroll);
        panel.add(scroll, BorderLayout.CENTER);
        refreshCustomButtonsGrid();
        return panel;
    }

    private JPanel buildAddForm() {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 16));
        card.setOpaque(true);
        card.putClientProperty("card", Boolean.TRUE);
        card.setBorder(new EmptyBorder(2, 14, 2, 14));

        JLabel nameLbl = smallLabel("Name:");
        JTextField nameField = new JTextField(12);
        nameField.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        styleInput(nameField);

        JLabel durLbl = smallLabel("Duration:");
        durLbl.setBorder(new EmptyBorder(0, 12, 0, 0));

        JTextField daysField  = intField();
        JTextField hoursField = intField();
        JTextField minsField  = intField();
        JTextField secsField  = intField();

        JLabel dLbl = smallLabel("d");
        JLabel hLbl = smallLabel("h");
        JLabel mLbl = smallLabel("m");
        JLabel sLbl = smallLabel("s");

        FlatButton addBtn = new FlatButton("+ Add", FlatButton.Role.SUCCESS, this, e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { warn("Please enter a name."); return; }
            if (name.length() > 60) { warn("Name too long (max 60 characters)."); return; }

            int d = parseSafe(daysField.getText());
            int h = parseSafe(hoursField.getText());
            int m = parseSafe(minsField.getText());
            int s = parseSafe(secsField.getText());

            if (d < 0 || h < 0 || m < 0 || s < 0) {
                warn("Please use whole numbers 0\u2013999 for days/hours and 0\u201359 for minutes/seconds.");
                return;
            }
            if (m > 59 || s > 59) {
                warn("Minutes and seconds must be between 0 and 59.");
                return;
            }

            long total = TimeUnit.DAYS.toMillis(d)
                       + TimeUnit.HOURS.toMillis(h)
                       + TimeUnit.MINUTES.toMillis(m)
                       + TimeUnit.SECONDS.toMillis(s);

            if (total <= 0) { warn("Duration must be greater than zero."); return; }
            if (total > TimeUnit.DAYS.toMillis(60)) { warn("Max duration is 60 days."); return; }

            customButtons.add(new CustomButtonDef(name, total));
            saveCustomButtons();
            refreshCustomButtonsGrid();

            nameField.setText("");
            daysField.setText("0");
            hoursField.setText("0");
            minsField.setText("0");
            secsField.setText("0");
            setStatus("Added custom button: " + name);
        });

        card.add(nameLbl);
        card.add(nameField);
        card.add(durLbl);
        card.add(daysField);  card.add(dLbl);
        card.add(hoursField); card.add(hLbl);
        card.add(minsField);  card.add(mLbl);
        card.add(secsField);  card.add(sLbl);
        card.add(Box.createHorizontalStrut(12));
        card.add(addBtn);

        return card;
    }

    private JTextField intField() {
        JTextField f = new JTextField("0", 3);
        f.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        f.setHorizontalAlignment(JTextField.CENTER);
        styleInput(f);
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { f.selectAll(); }
        });
        return f;
    }

    private JLabel smallLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT_UI, Font.PLAIN, 12));
        l.putClientProperty("subtext", Boolean.TRUE);
        return l;
    }

    private int parseSafe(String s) {
        if (s == null || s.isBlank()) return 0;
        try {
            int v = Integer.parseInt(s.trim());
            return v < 0 ? -1 : v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Invalid Input", JOptionPane.WARNING_MESSAGE);
    }

    private void refreshCustomButtonsGrid() {
        if (customButtonsContainer == null) return;
        customButtonsContainer.removeAll();

        if (customButtons.isEmpty()) {
            JLabel empty = new JLabel("No custom buttons yet. Add one above \u2191");
            empty.setFont(new Font(FONT_UI, Font.PLAIN, 13));
            empty.putClientProperty("subtext", Boolean.TRUE);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            customButtonsContainer.add(empty);
        } else {
            for (int i = 0; i < customButtons.size(); i++) {
                CustomButtonDef def = customButtons.get(i);
                final int idx = i;

                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.setBorder(new EmptyBorder(0, 0, 6, 0));

                FlatButton start = new FlatButton(
                        def.name + "  \u00B7  " + formatDuration(def.durationMillis),
                        FlatButton.Role.DEFAULT, this,
                        e -> startCustomTimer(def));
                start.setFont(new Font(FONT_UI, Font.PLAIN, 13));
                row.add(start, BorderLayout.CENTER);

                FlatButton del = new FlatButton("\u2715", FlatButton.Role.DANGER, this, e -> {
                    int choice = JOptionPane.showConfirmDialog(this,
                            "Delete custom button \"" + def.name + "\"?",
                            "Confirm Delete", JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (choice == JOptionPane.OK_OPTION) {
                        customButtons.remove(idx);
                        saveCustomButtons();
                        refreshCustomButtonsGrid();
                        setStatus("Deleted custom button: " + def.name);
                    }
                });
                del.setFont(new Font(FONT_UI, Font.BOLD, 13));
                del.setPreferredSize(new Dimension(52, 42));
                row.add(del, BorderLayout.EAST);

                customButtonsContainer.add(row);
            }
        }

        customButtonsContainer.revalidate();
        customButtonsContainer.repaint();
    }

    private void styleInput(JTextField field) {
        field.setBackground(darkMode ? new Color(40, 40, 48) : Color.WHITE);
        field.setForeground(cText);
        field.setCaretColor(cText);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cBorder, 1),
                new EmptyBorder(6, 8, 6, 8)));
    }

    // ===== ACTIVE TIMERS SCREEN =====
    private JPanel createActiveScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(32, 36, 24, 36));

        panel.add(buildScreenHeader("Active Timers",
                "Live countdowns. Select a row and cancel to remove it."), BorderLayout.NORTH);

        activeTimersList.setFont(new Font(FONT_UI, Font.PLAIN, 14));
        activeTimersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        activeTimersList.setFixedCellHeight(38);
        activeTimersList.setBorder(new EmptyBorder(6, 10, 6, 10));

        JScrollPane scroll = new JScrollPane(activeTimersList);
        scroll.setBorder(BorderFactory.createLineBorder(cBorder, 1));
        installModernScrollbar(scroll);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.setBorder(new EmptyBorder(14, 0, 0, 0));

        FlatButton cancel = new FlatButton("Cancel Selected", FlatButton.Role.DANGER, this, e -> {
            int idx = activeTimersList.getSelectedIndex();
            if (idx < 0 || idx >= activeTimers.size()) {
                JOptionPane.showMessageDialog(this, "Please select a timer to cancel.",
                        "No Selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            ActiveTimer target = activeTimers.get(idx);
            target.future.cancel(false);
            removeAndReportEnded(target, false);
            refreshActiveTimersList();
            if ("HOME".equals(currentScreen)) { statsDirty = true; refreshStatsUI(); }
            setStatus("Cancelled: " + target.label);
        });

        buttons.add(cancel);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    // ===== STAT TRACKING SCREEN =====
    private JPanel createStatTrackingScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(32, 36, 24, 36));

        panel.add(buildScreenHeader("Stat Tracking",
                "Look up Hypixel players or review your own timer activity."),
                BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);

        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabBar.setOpaque(false);
        tabBar.setBorder(new EmptyBorder(0, 0, 18, 0));

        final CardLayout innerLayout = new CardLayout();
        final JPanel inner = new JPanel(innerLayout);
        inner.setOpaque(false);

        TabButton profileTab = new TabButton("Player Profile", true,  this);
        TabButton timerTab   = new TabButton("Timer Stats",    false, this);

        profileTab.addActionListener(e -> {
            innerLayout.show(inner, "PROFILE");
            profileTab.setActive(true);
            timerTab.setActive(false);
            currentStatTab = "PROFILE";
        });
        timerTab.addActionListener(e -> {
            innerLayout.show(inner, "TIMER_STATS");
            profileTab.setActive(false);
            timerTab.setActive(true);
            currentStatTab = "TIMER_STATS";
            timerStatsDirty = true;
            refreshTimerStatsUI();
        });

        tabBar.add(profileTab);
        tabBar.add(timerTab);

        inner.add(buildProfileLookupPanel(), "PROFILE");
        inner.add(buildTimerStatsPanel(),    "TIMER_STATS");

        content.add(tabBar, BorderLayout.NORTH);
        content.add(inner,  BorderLayout.CENTER);

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildProfileLookupPanel() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);

        JPanel inputCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 14));
        inputCard.setOpaque(true);
        inputCard.putClientProperty("card", Boolean.TRUE);
        inputCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel userLbl = smallLabel("Username:");
        usernameField = new JTextField(16);
        usernameField.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        if (lastUsername != null && !lastUsername.isEmpty()) {
            usernameField.setText(lastUsername);
        }
        styleInput(usernameField);

        fetchStatsBtn = new FlatButton("Fetch Stats", FlatButton.Role.SUCCESS, this,
                e -> onFetchStatsClicked());

        inputCard.add(userLbl);
        inputCard.add(usernameField);
        inputCard.add(Box.createHorizontalStrut(12));
        inputCard.add(fetchStatsBtn);

        content.add(inputCard, BorderLayout.NORTH);

        statsContainer = new JPanel();
        statsContainer.setLayout(new BoxLayout(statsContainer, BoxLayout.Y_AXIS));
        statsContainer.setOpaque(false);
        statsContainer.setBorder(new EmptyBorder(18, 0, 0, 0));

        JLabel emptyPrompt = new JLabel("Enter a username and click Fetch Stats to view the profile.");
        emptyPrompt.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        emptyPrompt.putClientProperty("subtext", Boolean.TRUE);
        statsContainer.add(emptyPrompt);

        JScrollPane scroll = new JScrollPane(statsContainer);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        installModernScrollbar(scroll);
        content.add(scroll, BorderLayout.CENTER);

        return content;
    }

    private JPanel buildTimerStatsPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);

        timerStatsContainer = new JPanel();
        timerStatsContainer.setLayout(new BoxLayout(timerStatsContainer, BoxLayout.Y_AXIS));
        timerStatsContainer.setOpaque(false);
        timerStatsContainer.setBorder(new EmptyBorder(4, 0, 8, 0));

        JScrollPane scroll = new JScrollPane(timerStatsContainer);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        installModernScrollbar(scroll);

        wrap.add(scroll, BorderLayout.CENTER);

        refreshTimerStatsUI();
        return wrap;
    }

    private Map<String, Long> computeLiveDurations() {
        Map<String, Long> live = new TreeMap<>();
        long now = System.currentTimeMillis();
        synchronized (statsLock) {
            for (Map.Entry<String, Long> e : timerDurations.entrySet()) {
                live.put(e.getKey(), e.getValue());
            }
            for (ActiveTimer t : activeTimers) {
                long elapsed = now - t.startTimeMillis;
                if (elapsed > 0) live.merge(t.label, elapsed, Long::sum);
            }
        }
        return live;
    }

    private void refreshTimerStatsUI() {
        if (timerStatsContainer == null) return;
        timerStatsContainer.removeAll();

        Map<String, Long> live = computeLiveDurations();

        long totalTracked = getTotalActivityMillis();

        int totalStarted = 0;
        int totalCompleted = 0;
        int uniqueCount;
        synchronized (statsLock) {
            for (TimerStats s : timerStats.values()) {
                totalStarted += s.startedCount;
                totalCompleted += s.completedCount;
            }
            uniqueCount = live.size();
        }
        JPanel summary = new JPanel(new GridLayout(1, 4, 12, 0));
        summary.setOpaque(false);
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        summary.setPreferredSize(new Dimension(800, 86));

        summary.add(new InfoCard("TOTAL TIME",       formatDuration(totalTracked), "Wall-clock time tracking"));
        summary.add(new InfoCard("UNIQUE TIMERS",    String.valueOf(uniqueCount),  "Distinct labels"));
        summary.add(new InfoCard("TIMERS STARTED",   String.valueOf(totalStarted), "All-time count"));
        summary.add(new InfoCard("TIMERS COMPLETED", String.valueOf(totalCompleted),"Ran to zero"));

        timerStatsContainer.add(summary);
        timerStatsContainer.add(Box.createVerticalStrut(22));

        JLabel heading = sectionLabel("Time Per Timer");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        timerStatsContainer.add(heading);
        timerStatsContainer.add(Box.createVerticalStrut(12));

        if (live.isEmpty()) {
            JLabel empty = new JLabel("No timer activity yet \u2014 start any timer and it will show up here.");
            empty.setFont(new Font(FONT_UI, Font.PLAIN, 13));
            empty.putClientProperty("subtext", Boolean.TRUE);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            timerStatsContainer.add(empty);
        } else {
            List<Map.Entry<String, Long>> entries = new ArrayList<>(live.entrySet());
            entries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
            long maxDur = Math.max(1L, entries.get(0).getValue());

            for (Map.Entry<String, Long> e : entries) {
                timerStatsContainer.add(buildTimerStatRow(e.getKey(), e.getValue(), maxDur));
                timerStatsContainer.add(Box.createVerticalStrut(8));
            }
        }

        applyThemeToTree(timerStatsContainer);
        timerStatsContainer.revalidate();
        timerStatsContainer.repaint();
        timerStatsDirty = false;
    }

    private JPanel buildTimerStatRow(String label, long duration, long maxDuration) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(true);
        row.putClientProperty("card", Boolean.TRUE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel nameLbl = new JLabel(label);
        nameLbl.setFont(new Font(FONT_UI, Font.BOLD, 14));
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        TimerStats ts;
        synchronized (statsLock) {
            ts = timerStats.get(label);
        }
        int started   = ts != null ? ts.startedCount   : 0;
        int completed = ts != null ? ts.completedCount : 0;

        JLabel countsLbl = new JLabel(started + " started  \u2022  " + completed + " completed");
        countsLbl.setFont(new Font(FONT_UI, Font.PLAIN, 12));
        countsLbl.putClientProperty("subtext", Boolean.TRUE);
        countsLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        final long dur = duration;
        final long maxDur = maxDuration;
        final boolean dark = darkMode;
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(dark ? new Color(38, 38, 48) : new Color(226, 228, 234));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                double pct = Math.min(1.0, (double) dur / maxDur);
                int w = Math.max(4, (int) (getWidth() * pct));
                g2.setColor(new Color(60, 130, 220));
                g2.fillRoundRect(0, 0, w, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(100, 5));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(nameLbl);
        left.add(Box.createVerticalStrut(3));
        left.add(countsLbl);
        left.add(Box.createVerticalStrut(7));
        left.add(bar);

        row.add(left, BorderLayout.CENTER);

        JLabel durLbl = new JLabel(formatDuration(duration));
        durLbl.setFont(new Font(FONT_UI, Font.BOLD, 16));
        durLbl.setForeground(new Color(60, 130, 220));
        durLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel rightBox = new JPanel(new BorderLayout());
        rightBox.setOpaque(false);
        rightBox.setPreferredSize(new Dimension(180, 30));
        rightBox.setMinimumSize(new Dimension(180, 30));
        rightBox.setMaximumSize(new Dimension(180, 30));
        rightBox.setBorder(new EmptyBorder(0, 0, 0, 18));
        rightBox.add(durLbl, BorderLayout.CENTER);
        row.add(rightBox, BorderLayout.EAST);

        return row;
    }

    private void onFetchStatsClicked() {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            warn("Please enter a Minecraft username.");
            return;
        }

        lastUsername = username;
        prefs.put("lastUsername", username);

        fetchStatsBtn.setEnabled(false);

        statsContainer.removeAll();
        JLabel loading = new JLabel("Fetching data for " + username + "...");
        loading.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        loading.setForeground(cText);
        statsContainer.add(loading);
        statsContainer.revalidate();
        statsContainer.repaint();

        final String userFinal = username;

        currentFetch = fetchUuid(userFinal)
                .thenCompose(uuid -> {
                    updateStatOutput("UUID: " + uuid + "<br>Fetching Hypixel + SkyBlock data...");

                    CompletableFuture<Object> playerF = fetchPlayer(uuid, DEVELOPER_API_KEY);
                    CompletableFuture<Object> sbF     = fetchSkyblockProfiles(uuid, DEVELOPER_API_KEY);

                    return playerF.thenCombine(sbF, (player, skyblock) -> {
                        PlayerResult r = new PlayerResult();
                        r.uuid = uuid;
                        r.username = userFinal;
                        r.playerData = player;
                        r.skyblockData = skyblock;
                        return r;
                    });
                })
                .thenAccept(this::displayPlayerStats)
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    updateStatOutput("<b>Error:</b> " + cause.getMessage() + "<br><br>" +
                            "Common causes:<br>" +
                            "  &bull;  Wrong username (Mojang lookup failed)<br>" +
                            "  &bull;  Invalid developer API key<br>" +
                            "  &bull;  Rate limit hit<br>" +
                            "  &bull;  No internet connection");
                    SwingUtilities.invokeLater(() -> fetchStatsBtn.setEnabled(true));
                    return null;
                })
                .thenRun(() -> SwingUtilities.invokeLater(() -> fetchStatsBtn.setEnabled(true)));
    }

    private void updateStatOutput(String htmlText) {
        SwingUtilities.invokeLater(() -> {
            statsContainer.removeAll();
            JLabel err = new JLabel("<html><body style='width: 400px; font-family:" + FONT_UI + "'>" + htmlText + "</body></html>");
            err.setForeground(cText);
            statsContainer.add(err);
            statsContainer.revalidate();
            statsContainer.repaint();
        });
    }

    private static class PlayerResult {
        String uuid;
        String username;
        Object playerData;
        Object skyblockData;
    }

    private void displayPlayerStats(PlayerResult r) {
        SwingUtilities.invokeLater(() -> {
            statsContainer.removeAll();

            Object player = Json.getPath(r.playerData, "player");
            String name = r.username;
            double networkExp = 0;

            if (player != null) {
                String dn = Json.str(Json.getPath(player, "displayname"));
                if (dn != null) name = dn;
                networkExp = Json.dbl(Json.getPath(player, "networkExp"));
            }

            Object activeProfile = null;
            Object members = null;
            String cuteName = "None";

            Object profiles = Json.getPath(r.skyblockData, "profiles");
            if (profiles instanceof List) {
                List<?> list = (List<?>) profiles;
                for (Object p : list) {
                    if (Boolean.TRUE.equals(Json.getPath(p, "selected"))) {
                        activeProfile = p;
                        break;
                    }
                }
                if (activeProfile == null && !list.isEmpty()) activeProfile = list.get(0);

                if (activeProfile != null) {
                    cuteName = Json.str(Json.getPath(activeProfile, "cute_name"));
                    members = Json.getPath(activeProfile, "members");
                }
            }

            Object member = (members instanceof Map) ? ((Map<?, ?>) members).get(r.uuid) : null;

            int sbLevel = 0;
            double purse = 0;
            int fairySouls = 0;

            String topCropName = "None";   long topCropAmt = 0;
            String topBlockName = "None";  long topBlockAmt = 0;

            double totalSlayer = 0;
            double catacombsXp = 0;

            String highestSkillName = "None";
            double highestSkillXp = 0;
            double totalSkillXp = 0;

            if (member != null) {
                sbLevel = Json.integer(Json.getPath(member, "leveling", "experience")) / 100;
                purse = Json.dbl(Json.getPath(member, "currencies", "coin_purse"));
                fairySouls = Json.integer(Json.getPath(member, "fairy_soul", "total_collected"));

                Object collections = Json.getPath(member, "collection");
                if (collections instanceof Map) {
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) collections).entrySet()) {
                        String cName = String.valueOf(e.getKey());
                        long amt = ((Number) e.getValue()).longValue();

                        if (FARMING_CROPS.contains(cName) && amt > topCropAmt) {
                            topCropAmt = amt; topCropName = cName;
                        }
                        if (MINING_BLOCKS.contains(cName) && amt > topBlockAmt) {
                            topBlockAmt = amt; topBlockName = cName;
                        }
                    }
                }

                Object cataObj = Json.getPath(member, "dungeons", "dungeon_types", "catacombs", "experience");
                if (cataObj instanceof Number) catacombsXp = ((Number) cataObj).doubleValue();

                Object slayer = Json.getPath(member, "slayer", "slayer_bosses");
                if (slayer == null) slayer = Json.getPath(member, "slayer_bosses");
                if (slayer instanceof Map) {
                    for (Object bossObj : ((Map<?,?>)slayer).values()) {
                        totalSlayer += Json.dbl(Json.getPath(bossObj, "xp"));
                    }
                }

                String[] skills = {"combat", "mining", "farming", "foraging", "fishing", "enchanting", "alchemy", "taming"};
                for (String s : skills) {
                    double xp = Json.dbl(Json.getPath(member, "experience_skill_" + s));
                    if (xp <= 0) {
                        xp = Json.dbl(Json.getPath(member, "player_data", "experience", "SKILL_" + s.toUpperCase()));
                    }
                    totalSkillXp += xp;
                    if (xp > highestSkillXp) {
                        highestSkillXp = xp;
                        highestSkillName = titleCase(s);
                    }
                }
            }

            JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
            headerRow.setOpaque(false);
            headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            headerRow.add(new ImageOrbLabel(sbLevel, "xporb.gif", cBg));

            JPanel nameStack = new JPanel();
            nameStack.setLayout(new BoxLayout(nameStack, BoxLayout.Y_AXIS));
            nameStack.setOpaque(false);
            nameStack.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel nameLbl = new JLabel(name);
            nameLbl.setFont(new Font(FONT_UI, Font.BOLD, 28));
            nameLbl.setForeground(cText);

            JLabel profileLbl = new JLabel("Playing on " + cuteName + " \u2022 Network Lvl " + String.format("%.0f", expToLevel(networkExp)));
            profileLbl.setFont(new Font(FONT_UI, Font.PLAIN, 14));
            profileLbl.setForeground(cSubtext);

            nameStack.add(nameLbl);
            nameStack.add(Box.createVerticalStrut(4));
            nameStack.add(profileLbl);
            headerRow.add(nameStack);

            statsContainer.add(headerRow);
            statsContainer.add(Box.createVerticalStrut(24));

            JPanel grid = new JPanel(new GridLayout(0, 2, 14, 14));
            grid.setOpaque(false);
            grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
            grid.setAlignmentX(Component.LEFT_ALIGNMENT);

            grid.add(new InfoCard("CURRENT PURSE", formatCoins(purse) + " coins", "On active profile"));
            grid.add(new InfoCard("FAIRY SOULS", String.valueOf(fairySouls), "Total collected"));
            grid.add(new InfoCard("CATACOMBS XP", formatCoins(catacombsXp) + " XP", "Total dungeon experience"));
            grid.add(new InfoCard("TOTAL SLAYER XP", formatCoins(totalSlayer) + " XP", "All bosses combined"));
            grid.add(new InfoCard("HIGHEST SKILL", highestSkillName, formatCoins(highestSkillXp) + " XP"));
            grid.add(new InfoCard("TOTAL SKILLS XP", formatCoins(totalSkillXp) + " XP", "Across all main skills"));
            grid.add(new InfoCard("MOST FARMED CROP", titleCase(topCropName), formatCoins(topCropAmt) + " harvested"));
            grid.add(new InfoCard("MOST COLLECTED MINERAL", titleCase(topBlockName), formatCoins(topBlockAmt) + " collected"));

            statsContainer.add(grid);

            applyThemeToTree(statsContainer);

            statsContainer.revalidate();
            statsContainer.repaint();
        });
    }

    private static String titleCase(String s) {
        if (s == null || s.isEmpty()) return s;
        s = s.replace('_', ' ');
        String[] words = s.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0)))
              .append(w.substring(1))
              .append(' ');
        }
        return sb.toString().trim();
    }

    private static String formatCoins(double coins) {
        double abs = Math.abs(coins);
        String sign = coins < 0 ? "-" : "";
        if (abs >= 1_000_000_000) return sign + String.format("%.2fB", abs / 1_000_000_000.0);
        if (abs >= 1_000_000)     return sign + String.format("%.2fM", abs / 1_000_000.0);
        if (abs >= 1_000)         return sign + String.format("%.2fK", abs / 1_000.0);
        return String.format("%.0f", coins);
    }

    private static String formatDate(long epochMillis) {
        if (epochMillis <= 0) return "N/A";
        return new SimpleDateFormat("MMM d, yyyy").format(new Date(epochMillis));
    }

    private static double expToLevel(double exp) {
        if (exp <= 0) return 1.0;
        return (Math.sqrt(2 * exp + 30625) / 50) - 2.5;
    }

    // ===== PROFITS SCREEN =====
    private JPanel createProfitsScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(32, 36, 24, 36));

        panel.add(buildScreenHeader("Forge Profits",
                "Live Bazaar margins. Profit is recorded when each forge timer completes."),
                BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(0, 0, 14, 0));

        bazaarStatusLabel = new JLabel("Loading Bazaar prices...");
        bazaarStatusLabel.setFont(new Font(FONT_UI, Font.PLAIN, 12));
        bazaarStatusLabel.putClientProperty("subtext", Boolean.TRUE);

        refreshBazaarBtn = new FlatButton("Refresh Prices", FlatButton.Role.ACCENT, this,
                e -> fetchBazaarPrices());

        toolbar.add(bazaarStatusLabel, BorderLayout.WEST);
        toolbar.add(refreshBazaarBtn, BorderLayout.EAST);
        content.add(toolbar, BorderLayout.NORTH);

        profitsContainer = new JPanel();
        profitsContainer.setLayout(new BoxLayout(profitsContainer, BoxLayout.Y_AXIS));
        profitsContainer.setOpaque(false);

        buildForgeCalculatorSection(profitsContainer);
        profitsContainer.add(Box.createVerticalStrut(24));

        profitsSummaryPanel = new JPanel();
        profitsSummaryPanel.setLayout(new BoxLayout(profitsSummaryPanel, BoxLayout.Y_AXIS));
        profitsSummaryPanel.setOpaque(false);
        profitsSummaryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        profitsContainer.add(profitsSummaryPanel);

        profitsTablePanel = new JPanel();
        profitsTablePanel.setLayout(new BoxLayout(profitsTablePanel, BoxLayout.Y_AXIS));
        profitsTablePanel.setOpaque(false);
        profitsTablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        profitsContainer.add(profitsTablePanel);

        JScrollPane scroll = new JScrollPane(profitsContainer);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        installModernScrollbar(scroll);
        content.add(scroll, BorderLayout.CENTER);

        panel.add(content, BorderLayout.CENTER);
        refreshProfitsUI();
        return panel;
    }

    private void configureModernSpinner(JSpinner spinner) {
        spinner.setUI(new ModernSpinnerUI(this));
        spinner.setOpaque(false);
        spinner.setBackground(new Color(0, 0, 0, 0));
        spinner.setBorder(BorderFactory.createEmptyBorder());
        spinner.setFont(new Font(FONT_UI, Font.PLAIN, 13));

        if (spinner.getEditor() instanceof JSpinner.DefaultEditor) {
            JSpinner.DefaultEditor ed = (JSpinner.DefaultEditor) spinner.getEditor();
            ed.setOpaque(false);
            ed.setBackground(new Color(0, 0, 0, 0));
            JTextField tf = ed.getTextField();
            tf.setOpaque(false);
            tf.setBackground(new Color(0, 0, 0, 0));
            tf.setForeground(cText);
            tf.setCaretColor(cText);
            tf.setBorder(new EmptyBorder(0, 14, 0, 0));
            tf.setFont(new Font(FONT_UI, Font.PLAIN, 13));
            tf.setHorizontalAlignment(JTextField.LEFT);
        }
    }

    private void buildForgeCalculatorSection(JPanel parent) {
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel headWrap = new JPanel(new BorderLayout(8, 0));
        headWrap.setOpaque(false);
        headWrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_GOLD);
                g2.fillRoundRect(0, 2, 4, Math.max(4, getHeight() - 4), 3, 3);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(6, 22));

        JLabel heading = new JLabel("Forge Time Profit Calculator");
        heading.setFont(new Font(FONT_UI, Font.BOLD, 17));
        heading.setForeground(C_GOLD);

        headWrap.add(bar, BorderLayout.WEST);
        headWrap.add(heading, BorderLayout.CENTER);

        wrap.add(headWrap);
        wrap.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("Estimate profit before you start. Uses current Bazaar prices.");
        sub.setFont(new Font(FONT_UI, Font.PLAIN, 12));
        sub.putClientProperty("subtext", Boolean.TRUE);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(sub);
        wrap.add(Box.createVerticalStrut(14));

        JPanel inputCardWrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight(), arc = 14;
                g2.setPaint(new GradientPaint(0, 0, new Color(42, 44, 54), 0, h, new Color(30, 31, 40)));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(new Color(255, 200, 90, 55));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
            }
        };
        inputCardWrap.setOpaque(false);
        inputCardWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputCardWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        inputCardWrap.setPreferredSize(new Dimension(100, 76));
        inputCardWrap.setBorder(new EmptyBorder(0, 10, 0, 10));

        JPanel inputInner = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 18));
        inputInner.setOpaque(false);

        JLabel recipeLbl = smallLabel("Recipe:");
        calcRecipeCombo = new JComboBox<>();
        calcRecipeCombo.setUI(new ModernComboBoxUI(HypixelTimerApp.this));
        calcRecipeCombo.setOpaque(false);
        calcRecipeCombo.setForeground(cText);
        calcRecipeCombo.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        calcRecipeCombo.setBorder(new EmptyBorder(0, 14, 0, 0));
        for (String key : FORGE_RECIPES.keySet()) {
            calcRecipeCombo.addItem(titleCase(key.toLowerCase()));
        }
        RoundedInputWrapper comboWrapper = new RoundedInputWrapper(calcRecipeCombo, 210, 34);

        JLabel qtyLbl = smallLabel("Quantity:");
        calcQtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        configureModernSpinner(calcQtySpinner);
        RoundedInputWrapper qtyWrapper = new RoundedInputWrapper(calcQtySpinner, 90, 34);

        JLabel slotLbl = smallLabel("Slots:");
        calcSlotsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 7, 1));
        configureModernSpinner(calcSlotsSpinner);
        RoundedInputWrapper slotWrapper = new RoundedInputWrapper(calcSlotsSpinner, 70, 34);

        inputInner.add(recipeLbl);
        inputInner.add(comboWrapper);
        inputInner.add(Box.createHorizontalStrut(6));
        inputInner.add(qtyLbl);
        inputInner.add(qtyWrapper);
        inputInner.add(Box.createHorizontalStrut(6));
        inputInner.add(slotLbl);
        inputInner.add(slotWrapper);

        inputCardWrap.add(inputInner, BorderLayout.WEST);
        wrap.add(inputCardWrap);
        wrap.add(Box.createVerticalStrut(14));

        JPanel resultsGrid = new JPanel(new GridLayout(2, 3, 12, 12));
        resultsGrid.setOpaque(false);
        resultsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        calcCardCost    = new CalcResultCard("TOTAL INPUT COST",   C_ORANGE);
        calcCardOut     = new CalcResultCard("TOTAL OUTPUT VALUE", C_BLUE);
        calcCardProfit  = new CalcResultCard("NET PROFIT",         C_GREEN);
        calcCardTime    = new CalcResultCard("TOTAL FORGE TIME",   C_GOLD);
        calcCardPerHour = new CalcResultCard("PROFIT PER HOUR",    C_GREEN);
        calcCardRoi     = new CalcResultCard("RETURN ON INVESTMENT", C_GOLD);

        resultsGrid.add(calcCardCost);
        resultsGrid.add(calcCardOut);
        resultsGrid.add(calcCardProfit);
        resultsGrid.add(calcCardTime);
        resultsGrid.add(calcCardPerHour);
        resultsGrid.add(calcCardRoi);

        wrap.add(resultsGrid);

        calcRecipeCombo.addActionListener(e -> refreshCalculatorValues());
        calcQtySpinner.addChangeListener(e -> refreshCalculatorValues());
        calcSlotsSpinner.addChangeListener(e -> refreshCalculatorValues());

        parent.add(wrap);
    }

    private void refreshCalculatorValues() {
        if (calcRecipeCombo == null || calcQtySpinner == null || calcSlotsSpinner == null) return;

        String sel = (String) calcRecipeCombo.getSelectedItem();
        if (sel == null) return;

        String recipeKey = null;
        for (String k : FORGE_RECIPES.keySet()) {
            if (titleCase(k.toLowerCase()).equals(sel)) { recipeKey = k; break; }
        }
        if (recipeKey == null) return;

        int qty = ((Number) calcQtySpinner.getValue()).intValue();
        if (qty < 1) qty = 1;
        int slots = ((Number) calcSlotsSpinner.getValue()).intValue();
        if (slots < 1) slots = 1;

        Map<String, Integer> recipe = FORGE_RECIPES.get(recipeKey);
        if (recipe == null) return;

        boolean hasPrices = !bazaarBuyPrices.isEmpty() || !bazaarSellPrices.isEmpty();

        double itemCost   = computeRecipeCost(recipe);
        double itemValue  = Json.dbl(bazaarSellPrices.get(recipeKey));
        double itemProfit = itemCost == Double.NEGATIVE_INFINITY
                ? Double.NEGATIVE_INFINITY
                : itemValue - itemCost;

        double totalCost   = itemCost   == Double.NEGATIVE_INFINITY ? 0 : itemCost   * qty;
        double totalValue  = itemValue  * qty;
        double totalProfit = itemProfit == Double.NEGATIVE_INFINITY ? 0 : itemProfit * qty;

        Long durPerItem = FORGE_DURATIONS.get(recipeKey);
        long perItemMillis = durPerItem != null ? durPerItem : 0;

        int batches = (qty + slots - 1) / slots;
        long totalMillis = perItemMillis * batches;

        double hours = totalMillis / 3_600_000.0;
        double perHour = hours > 0 ? totalProfit / hours : 0;
        double roi = totalCost > 0 ? (totalProfit / totalCost) * 100.0 : 0;

        boolean costMissing = itemCost == Double.NEGATIVE_INFINITY;
        boolean valueMissing = itemValue <= 0;

        if (!hasPrices || costMissing || valueMissing) {
            calcCardCost.setValue("\u2014", C_ORANGE);
            calcCardOut.setValue("\u2014", C_BLUE);
            calcCardProfit.setValue("\u2014", C_GREEN);
            calcCardTime.setValue(totalMillis > 0 ? formatDuration(totalMillis) : "\u2014", C_GOLD);
            calcCardPerHour.setValue("\u2014", C_GREEN);
            calcCardRoi.setValue("\u2014", C_GOLD);
        } else {
            calcCardCost.setValue(formatCoins(totalCost) + " coins", C_ORANGE);
            calcCardOut.setValue(formatCoins(totalValue) + " coins", C_BLUE);
            calcCardProfit.setValue((totalProfit >= 0 ? "+" : "") + formatCoins(totalProfit) + " coins",
                    totalProfit >= 0 ? C_GREEN : C_RED);
            calcCardTime.setValue(totalMillis > 0 ? formatDuration(totalMillis) : "\u2014", C_GOLD);
            calcCardPerHour.setValue(hours > 0 ? formatCoins(perHour) + " /hr" : "\u2014",
                    perHour >= 0 ? C_GREEN : C_RED);
            calcCardRoi.setValue(String.format("%.1f%%", roi),
                    roi >= 0 ? C_GREEN : C_RED);
        }

        if (slots > 1) {
            calcCardTime.setTitle("TOTAL FORGE TIME \u00D7" + slots + " SLOTS");
        } else {
            calcCardTime.setTitle("TOTAL FORGE TIME");
        }
    }

    private void fetchBazaarPrices() {
    	    if (!ensureApiKey()) return;

        if (refreshBazaarBtn != null) refreshBazaarBtn.setEnabled(false);
        if (bazaarStatusLabel != null) bazaarStatusLabel.setText("Fetching Bazaar prices...");

        httpGet("https://api.hypixel.net/v2/skyblock/bazaar", DEVELOPER_API_KEY)
                .thenApply(Json::parse)
                .thenAccept(root -> {
                    if (root == null) {
                        SwingUtilities.invokeLater(() -> {
                            if (bazaarStatusLabel != null)
                                bazaarStatusLabel.setText("Bazaar data unavailable.");
                            if (refreshBazaarBtn != null) refreshBazaarBtn.setEnabled(true);
                        });
                        return;
                    }
                    Object products = Json.getPath(root, "products");
                    if (!(products instanceof Map)) {
                        SwingUtilities.invokeLater(() -> {
                            if (bazaarStatusLabel != null)
                                bazaarStatusLabel.setText("Malformed Bazaar response.");
                            if (refreshBazaarBtn != null) refreshBazaarBtn.setEnabled(true);
                        });
                        return;
                    }

                    Map<String, Double> buyMap  = new java.util.HashMap<>();
                    Map<String, Double> sellMap = new java.util.HashMap<>();

                    for (Map.Entry<?, ?> e : ((Map<?, ?>) products).entrySet()) {
                        String pid = String.valueOf(e.getKey());
                        Object prod = e.getValue();
                        Object qs = Json.getPath(prod, "quick_status");
                        if (qs == null) continue;

                        double buyPrice  = Json.dbl(Json.getPath(qs, "buyPrice"));
                        double sellPrice = Json.dbl(Json.getPath(qs, "sellPrice"));
                        if (buyPrice  > 0) buyMap.put(pid, buyPrice);
                        if (sellPrice > 0) sellMap.put(pid, sellPrice);
                    }

                    SwingUtilities.invokeLater(() -> {
                        bazaarBuyPrices  = buyMap;
                        bazaarSellPrices = sellMap;
                        bazaarLastUpdated = System.currentTimeMillis();
                        if (bazaarStatusLabel != null)
                            bazaarStatusLabel.setText("Prices updated " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
                        if (refreshBazaarBtn != null) refreshBazaarBtn.setEnabled(true);

                        if (!pendingProfitRecords.isEmpty()) {
                            Map<String, Integer> queued = new java.util.HashMap<>(pendingProfitRecords);
                            pendingProfitRecords.clear();
                            savePendingProfits();
                            int totalQueued = 0;
                            for (int n : queued.values()) totalQueued += n;
                            devLog("[profit] Draining " + totalQueued
                                    + " queued record" + (totalQueued == 1 ? "" : "s")
                                    + " across " + queued.size() + " recipe(s)");
                            for (Map.Entry<String, Integer> qe : queued.entrySet()) {
                                for (int i = 0; i < qe.getValue(); i++) {
                                    recordProfit(qe.getKey());
                                }
                            }
                        }

                        refreshProfitsUI();
                    });
                })
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        if (bazaarStatusLabel != null)
                            bazaarStatusLabel.setText("Failed to fetch prices: " + ex.getMessage());
                        if (refreshBazaarBtn != null) refreshBazaarBtn.setEnabled(true);
                    });
                    return null;
                });
    }

    private void refreshProfitsUI() {
        if (profitsSummaryPanel == null || profitsTablePanel == null) return;

        refreshCalculatorValues();

        profitsSummaryPanel.removeAll();

        JPanel summary = new JPanel(new GridLayout(1, 3, 12, 0));
        summary.setOpaque(false);
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        summary.setPreferredSize(new Dimension(800, 86));

        int pendingTotal = 0;
        for (int n : pendingProfitRecords.values()) pendingTotal += n;

        String profitSub = pendingTotal > 0
                ? pendingTotal + " pending (waiting on prices)"
                : "All-time tracked";

        summary.add(new InfoCard("TOTAL PROFIT",
                formatCoins(totalProfitCoins) + " coins", profitSub));
        summary.add(new InfoCard("UNIQUE RECIPES", String.valueOf(perRecipeProfit.size()), "Profited from"));
        summary.add(new InfoCard("BAZAAR STATUS",
                bazaarBuyPrices.isEmpty() ? "No data" : "Live",
                bazaarLastUpdated > 0
                        ? new SimpleDateFormat("HH:mm:ss").format(new Date(bazaarLastUpdated))
                        : "Not fetched"));

        profitsSummaryPanel.add(summary);
        applyThemeToTree(profitsSummaryPanel);
        profitsSummaryPanel.revalidate();
        profitsSummaryPanel.repaint();

        profitsTablePanel.removeAll();

        JLabel heading = sectionLabel("Per-Recipe Margins");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        profitsTablePanel.add(heading);
        profitsTablePanel.add(Box.createVerticalStrut(10));

        if (bazaarBuyPrices.isEmpty()) {
            JLabel empty = new JLabel("Bazaar prices not loaded yet. Click Refresh Prices.");
            empty.setFont(new Font(FONT_UI, Font.PLAIN, 13));
            empty.putClientProperty("subtext", Boolean.TRUE);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            profitsTablePanel.add(empty);
        } else {
            JPanel headerRow = new JPanel(new GridLayout(1, 6, 8, 0));
            headerRow.setOpaque(false);
            headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            headerRow.setBorder(new EmptyBorder(0, 12, 6, 12));

            String[] hdrs = {"RECIPE", "INPUT COST", "OUTPUT VALUE", "MARGIN", "PROFIT/HR", "TRACKED"};
            for (String h : hdrs) {
                JLabel l = new JLabel(h, h.equals("RECIPE") ? SwingConstants.LEFT : SwingConstants.RIGHT);
                l.setFont(new Font(FONT_UI, Font.BOLD, 10));
                l.putClientProperty("nextUpSubtext", Boolean.TRUE);
                headerRow.add(l);
            }
            profitsTablePanel.add(headerRow);

            JSeparator sep = new JSeparator();
            sep.setForeground(cBorder);
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            profitsTablePanel.add(sep);

            List<String> sorted = new ArrayList<>();
            for (String key : FORGE_RECIPES.keySet()) {
                double out = Json.dbl(bazaarSellPrices.get(key));
                if (out > 0) sorted.add(key);
            }
            sorted.sort((a, b) -> {
                double pa = computeRecipeProfitPerHour(a);
                double pb = computeRecipeProfitPerHour(b);
                return Double.compare(pb, pa);
            });

            for (String key : sorted) {
                Map<String, Integer> recipe = FORGE_RECIPES.get(key);
                if (recipe == null) continue;

                double inputCost = computeRecipeCost(recipe);
                double outputValue = Json.dbl(bazaarSellPrices.get(key));
                double margin = inputCost == Double.NEGATIVE_INFINITY
                        ? Double.NEGATIVE_INFINITY
                        : outputValue - inputCost;
                double perHour = computeRecipeProfitPerHour(key);
                double tracked = perRecipeProfit.getOrDefault(key, 0.0);

                JPanel row = new JPanel(new GridLayout(1, 6, 8, 0));
                row.setOpaque(true);
                row.putClientProperty("tableRow", Boolean.TRUE);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                row.setBorder(new EmptyBorder(6, 12, 6, 12));

                JLabel nameL = new JLabel(titleCase(key.toLowerCase()));
                nameL.setFont(new Font(FONT_UI, Font.BOLD, 11));
                nameL.setVerticalAlignment(SwingConstants.CENTER);

                boolean costMissing = inputCost == Double.NEGATIVE_INFINITY;

                JLabel costL = new JLabel(costMissing ? "\u2014" : formatCoins(inputCost));
                costL.setFont(new Font(FONT_UI, Font.PLAIN, 11));
                costL.setHorizontalAlignment(SwingConstants.RIGHT);
                costL.setVerticalAlignment(SwingConstants.CENTER);
                costL.putClientProperty("subtext", Boolean.TRUE);

                JLabel outL = new JLabel(formatCoins(outputValue));
                outL.setFont(new Font(FONT_UI, Font.PLAIN, 11));
                outL.setHorizontalAlignment(SwingConstants.RIGHT);
                outL.setVerticalAlignment(SwingConstants.CENTER);
                outL.putClientProperty("subtext", Boolean.TRUE);

                JLabel marginL = new JLabel(costMissing ? "\u2014" : formatCoins(margin));
                marginL.setFont(new Font(FONT_UI, Font.BOLD, 11));
                marginL.setHorizontalAlignment(SwingConstants.RIGHT);
                marginL.setVerticalAlignment(SwingConstants.CENTER);
                marginL.setForeground(costMissing ? cSubtext : (margin >= 0 ? C_GREEN : C_RED));

                JLabel perHrL = new JLabel(formatCoins(perHour));
                perHrL.setFont(new Font(FONT_UI, Font.BOLD, 11));
                perHrL.setHorizontalAlignment(SwingConstants.RIGHT);
                perHrL.setVerticalAlignment(SwingConstants.CENTER);
                perHrL.setForeground(perHour >= 0 ? C_GREEN : C_RED);

                JLabel trackedL = new JLabel(tracked > 0 ? formatCoins(tracked) : "\u2014");
                trackedL.setFont(new Font(FONT_UI, Font.PLAIN, 11));
                trackedL.setHorizontalAlignment(SwingConstants.RIGHT);
                trackedL.setVerticalAlignment(SwingConstants.CENTER);
                trackedL.putClientProperty("subtext", Boolean.TRUE);

                row.add(nameL); row.add(costL); row.add(outL); row.add(marginL);
                row.add(perHrL); row.add(trackedL);
                profitsTablePanel.add(row);
                profitsTablePanel.add(Box.createVerticalStrut(4));
            }
        }

        applyThemeToTree(profitsTablePanel);
        profitsTablePanel.revalidate();
        profitsTablePanel.repaint();
    }

    private double computeRecipeCost(Map<String, Integer> recipe) {
        double total = 0;
        for (Map.Entry<String, Integer> e : recipe.entrySet()) {
            Double price = bazaarBuyPrices.get(e.getKey());
            if (price == null || price <= 0) return Double.NEGATIVE_INFINITY;
            total += price * e.getValue();
        }
        return total;
    }

    private double computeRecipeProfit(String recipeKey) {
        Map<String, Integer> recipe = FORGE_RECIPES.get(recipeKey);
        if (recipe == null) return 0;
        double cost = computeRecipeCost(recipe);
        if (cost == Double.NEGATIVE_INFINITY) return Double.NEGATIVE_INFINITY;
        double value = Json.dbl(bazaarSellPrices.get(recipeKey));
        if (value <= 0) return Double.NEGATIVE_INFINITY;
        return value - cost;
    }

    private double computeRecipeProfitPerHour(String recipeKey) {
        double profitPerItem = computeRecipeProfit(recipeKey);
        if (profitPerItem == Double.NEGATIVE_INFINITY) return 0;
        Long durMillis = FORGE_DURATIONS.get(recipeKey);
        if (durMillis == null || durMillis <= 0) return 0;
        double hours = durMillis / 3_600_000.0;
        return profitPerItem / hours;
    }

    private void recordProfit(String recipeKey) {
        if (bazaarSellPrices.isEmpty() || bazaarBuyPrices.isEmpty()) {
            pendingProfitRecords.merge(recipeKey, 1, Integer::sum);
            savePendingProfits();
            devLog("[profit] Queued \"" + recipeKey + "\" ("
                    + pendingProfitRecords.get(recipeKey) + " pending)");
            if ("PROFITS".equals(currentScreen)) {
                SwingUtilities.invokeLater(this::refreshProfitsUI);
            }
            return;
        }

        double profit = computeRecipeProfit(recipeKey);
        if (profit == Double.NEGATIVE_INFINITY) {
            pendingProfitRecords.merge(recipeKey, 1, Integer::sum);
            savePendingProfits();
            devLog("[profit] Queued \"" + recipeKey + "\" (missing ingredient price)");
            if ("PROFITS".equals(currentScreen)) {
                SwingUtilities.invokeLater(this::refreshProfitsUI);
            }
            return;
        }
        if (profit <= 0) {
            devLog("[profit] Skipped \"" + recipeKey + "\" (margin "
                    + formatCoins(profit) + ")");
            return;
        }
        synchronized (statsLock) {
            totalProfitCoins += profit;
            perRecipeProfit.merge(recipeKey, profit, Double::sum);
            saveProfitData();
        }
        devLog("[profit] +" + formatCoins(profit) + " from \"" + recipeKey
                + "\"  \u2192  total " + formatCoins(totalProfitCoins));
        if ("PROFITS".equals(currentScreen)) {
            SwingUtilities.invokeLater(this::refreshProfitsUI);
        }
    }

    private void saveProfitData() {
        prefs.putDouble("profit.total", totalProfitCoins);
        for (Map.Entry<String, Double> e : perRecipeProfit.entrySet()) {
            String enc = URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8);
            prefs.putDouble("profit.recipe." + enc, e.getValue());
        }
    }

    private void savePendingProfits() {
        prefs.putInt("profit.pending.count", pendingProfitRecords.size());
        int i = 0;
        for (Map.Entry<String, Integer> e : pendingProfitRecords.entrySet()) {
            String enc = URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8);
            prefs.put("profit.pending." + i + ".key", enc);
            prefs.putInt("profit.pending." + i + ".count", e.getValue());
            i++;
        }
        for (int j = i; j < 200; j++) {
            if (prefs.get("profit.pending." + j + ".key", null) == null) break;
            prefs.remove("profit.pending." + j + ".key");
            prefs.remove("profit.pending." + j + ".count");
        }
    }

    private void loadPendingProfits() {
        pendingProfitRecords.clear();
        int count = prefs.getInt("profit.pending.count", 0);
        for (int i = 0; i < count; i++) {
            String enc = prefs.get("profit.pending." + i + ".key", null);
            if (enc == null) continue;
            int n = prefs.getInt("profit.pending." + i + ".count", 0);
            if (n <= 0) continue;
            try {
                String key = URLDecoder.decode(enc, StandardCharsets.UTF_8);
                pendingProfitRecords.put(key, n);
            } catch (Exception ignored) {}
        }
    }

    private void loadPerRecipeProfit() {
        try {
            for (String key : prefs.keys()) {
                if (key.startsWith("profit.recipe.")) {
                    String enc = key.substring("profit.recipe.".length());
                    String recipe = URLDecoder.decode(enc, StandardCharsets.UTF_8);
                    double val = prefs.getDouble(key, 0);
                    if (val > 0) perRecipeProfit.put(recipe, val);
                }
            }
        } catch (BackingStoreException ignored) {}
    }

    // ===== SETTINGS SCREEN =====
    private JPanel createSettingsScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(32, 36, 24, 36));

        panel.add(buildScreenHeader("Settings",
                "Personalize the app. Changes save automatically."), BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        form.add(sectionLabel("Appearance"));
        form.add(Box.createVerticalStrut(10));

        lightRadio = new JRadioButton("Light");
        darkRadio  = new JRadioButton("Dark");
        lightRadio.setFont(new Font(FONT_UI, Font.PLAIN, 14));
        darkRadio.setFont(new Font(FONT_UI, Font.PLAIN, 14));
        lightRadio.setOpaque(false);
        darkRadio.setOpaque(false);
        lightRadio.setFocusPainted(false);
        darkRadio.setFocusPainted(false);
        ButtonGroup g1 = new ButtonGroup();
        g1.add(lightRadio); g1.add(darkRadio);
        if (darkMode) darkRadio.setSelected(true); else lightRadio.setSelected(true);

        ActionListener themeListener = e -> {
            boolean newDark = darkRadio.isSelected();
            if (newDark == darkMode) return;
            darkMode = newDark;
            prefs.putBoolean("darkMode", darkMode);
            applyTheme();
        };
        lightRadio.addActionListener(themeListener);
        darkRadio.addActionListener(themeListener);

        JPanel themeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        themeRow.setOpaque(false);
        themeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeRow.add(lightRadio);
        themeRow.add(darkRadio);
        form.add(themeRow);

        form.add(Box.createVerticalStrut(24));
        form.add(sectionLabel("Timer Sound"));
        form.add(Box.createVerticalStrut(6));

        JLabel soundHint = new JLabel("Rings on loop until you dismiss the popup (max 60s).");
        soundHint.setFont(new Font(FONT_UI, Font.PLAIN, 12));
        soundHint.putClientProperty("subtext", Boolean.TRUE);
        soundHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(soundHint);
        form.add(Box.createVerticalStrut(8));

        settingsSoundCombo = new JComboBox<>(new String[]{
                "None", "Beep", "Chime", "Bell",
                "Coin", "Level Up", "Anvil", "Villager",
                "Alarm", "Siren", "Portal",
                "Dopamine",
                "Custom Song"
        });
        settingsSoundCombo.setUI(new ModernComboBoxUI(HypixelTimerApp.this));
        settingsSoundCombo.setOpaque(false);
        settingsSoundCombo.setForeground(cText);
        settingsSoundCombo.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        settingsSoundCombo.setBorder(new EmptyBorder(0, 14, 0, 0));
        settingsSoundCombo.setSelectedItem(soundChoice);
        settingsSoundCombo.addActionListener(e -> {
            soundChoice = (String) settingsSoundCombo.getSelectedItem();
            prefs.put("sound", soundChoice);
        });

        RoundedInputWrapper soundComboWrapper = new RoundedInputWrapper(settingsSoundCombo, 200, 34);
        FlatButton testBtn = new FlatButton("Test Sound", FlatButton.Role.ACCENT, this, e -> playSound());
        FlatButton alertBtn = new FlatButton("Test Alert", FlatButton.Role.SUCCESS, this, e -> {
            playSound();
            showDesktopNotification("Test Alert",
                    "If you can see this, desktop notifications are working!");
        });
        FlatButton stopBtn = new FlatButton("Stop", FlatButton.Role.DEFAULT, this, e -> stopSound());

        JPanel soundRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        soundRow.setOpaque(false);
        soundRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        soundRow.add(soundComboWrapper);
        soundRow.add(testBtn);
        soundRow.add(alertBtn);
        soundRow.add(stopBtn);
        form.add(soundRow);
        form.add(Box.createVerticalStrut(14));

        JPanel volRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        volRow.setOpaque(false);
        volRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel volLbl = smallLabel("Volume:");
        volLbl.setPreferredSize(new Dimension(70, 20));

        JSlider volSlider = new JSlider(0, 100, volume);
        volSlider.setOpaque(false);
        volSlider.setPreferredSize(new Dimension(240, 30));
        volSlider.setFocusable(false);

        JLabel volValue = new JLabel(volume + "%");
        volValue.setFont(new Font(FONT_UI, Font.BOLD, 13));
        volValue.setPreferredSize(new Dimension(46, 20));

        volSlider.addChangeListener(e -> {
            volume = volSlider.getValue();
            volValue.setText(volume + "%");
            prefs.putInt("volume", volume);
        });

        volRow.add(volLbl);
        volRow.add(volSlider);
        volRow.add(volValue);
        form.add(volRow);

        form.add(Box.createVerticalStrut(14));

        JPanel songRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        songRow.setOpaque(false);
        songRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel songLbl = smallLabel("Custom song:");
        songLbl.setPreferredSize(new Dimension(90, 20));

        String initialSong = (customSongPath == null || customSongPath.isEmpty())
                ? "(none selected)"
                : new File(customSongPath).getName();

        JLabel songPath = new JLabel(initialSong);
        songPath.setFont(new Font(FONT_UI, Font.PLAIN, 12));
        songPath.putClientProperty("subtext", Boolean.TRUE);
        songPath.setPreferredSize(new Dimension(260, 20));
        songPath.setToolTipText(customSongPath);

        FlatButton browseBtn = new FlatButton("Browse...", FlatButton.Role.DEFAULT, this, e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose a WAV audio file");
            chooser.setFileFilter(new FileNameExtensionFilter(
                    "WAV audio (*.wav, *.au, *.aiff)", "wav", "au", "aiff", "aif"));
            if (customSongPath != null && !customSongPath.isEmpty()) {
                File cur = new File(customSongPath);
                if (cur.getParentFile() != null) chooser.setCurrentDirectory(cur.getParentFile());
            }
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                customSongPath = chooser.getSelectedFile().getAbsolutePath();
                prefs.put("customSongPath", customSongPath);
                songPath.setText(new File(customSongPath).getName());
                songPath.setToolTipText(customSongPath);
                settingsSoundCombo.setSelectedItem("Custom Song");
                soundChoice = "Custom Song";
                prefs.put("sound", soundChoice);
                setStatus("Custom song set: " + new File(customSongPath).getName());
            }
        });

        songRow.add(songLbl);
        songRow.add(songPath);
        songRow.add(browseBtn);
        form.add(songRow);

        JLabel songHint = new JLabel("Only WAV / AIFF / AU are supported natively by the JVM. "
                + "Convert MP3s first (e.g. with Audacity).");
        songHint.setFont(new Font(FONT_UI, Font.PLAIN, 11));
        songHint.putClientProperty("subtext", Boolean.TRUE);
        songHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(Box.createVerticalStrut(4));
        form.add(songHint);
        form.add(Box.createVerticalStrut(24));
        form.add(sectionLabel("Statistics"));
        form.add(Box.createVerticalStrut(10));

        FlatButton resetBtn = new FlatButton("Reset Statistics", FlatButton.Role.DANGER, this, e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Reset all timer statistics? This cannot be undone.",
                    "Confirm Reset", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) return;
            resetStats();
            setStatus("Statistics reset.");
        });
        resetBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(resetBtn);

        form.add(Box.createVerticalStrut(24));

        JLabel note = new JLabel("Tip: timers keep running even when you're on another screen.");
        note.setFont(new Font(FONT_UI, Font.PLAIN, 12));
        note.putClientProperty("subtext", Boolean.TRUE);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(note);

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrapper.setOpaque(false);
        wrapper.add(form);
        panel.add(wrapper, BorderLayout.CENTER);

        return panel;
    }

    // ===== SHARED HEADER =====
    private JPanel buildScreenHeader(String title, String subtitle) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel t = new JLabel(title);
        t.setFont(new Font(FONT_UI, Font.BOLD, 26));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(t);
        return header;
    }

    // ===== THEME =====
    private void applyTheme() {
        updateThemeColors();

        getContentPane().setBackground(cBg);
        screens.setBackground(cBg);

        sidebar.setBackground(cSidebarBg);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, cSidebarBorder));

        statusBar.setBackground(cStatusBg);
        statusBar.setForeground(cSubtext);
        for (FlatButton b : flatButtons) b.setDark(darkMode);
        for (NavButton nb : navButtons) nb.setDark(darkMode);
        for (StatCard sc : statCards) sc.applyTheme(darkMode, cPanel, cText, cSubtext, cBorder);
        for (TabButton tb : tabButtons) tb.repaint();
        for (JScrollPane sp : themedScrollPanes) applyScrollbarTheme(sp);
        for (RoundedInputWrapper rw : roundedWrappers) rw.repaint();

        if (calcRecipeCombo != null) {
            calcRecipeCombo.setForeground(cText);
            calcRecipeCombo.repaint();
        }
        if (settingsSoundCombo != null) {
            settingsSoundCombo.setForeground(cText);
            if (settingsSoundCombo.getUI() instanceof ModernComboBoxUI) {
                ((ModernComboBoxUI) settingsSoundCombo.getUI()).applyTheme();
            }
            settingsSoundCombo.repaint();
        }
        if (calcRecipeCombo != null
                && calcRecipeCombo.getUI() instanceof ModernComboBoxUI) {
            ((ModernComboBoxUI) calcRecipeCombo.getUI()).applyTheme();
        }
        if (calcQtySpinner != null) configureModernSpinner(calcQtySpinner);
        if (calcSlotsSpinner != null) configureModernSpinner(calcSlotsSpinner);

        applyThemeToTree(screens);
        applyThemeToTree(sidebar);

        if (nextUpBox != null) {
            nextUpBox.setBackground(cPanelAlt);
            nextUpBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, cAccentStripe),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(cBorder, 1),
                            new EmptyBorder(14, 18, 14, 18))));
        }

        if (nextUpTitleLabel != null) nextUpTitleLabel.setForeground(cText);
        if (nextUpTimeLabel  != null) nextUpTimeLabel.setForeground(cAccentStripe);

        statsDirty = true;
        timerStatsDirty = true;
        refreshCustomButtonsGrid();
        refreshProfitsUI();

        if (darkMode) trySetDarkTitleBar();
    }

    private void applyThemeToTree(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof NavButton) continue;
            if (comp instanceof TabButton) continue;
            if (comp instanceof StatCard) continue;
            if (comp instanceof HeroCard) continue;
            if (comp instanceof CalcResultCard) continue;
            if (comp instanceof RoundedInputWrapper) continue;
            if (comp instanceof MinecraftTitleLabel) continue;
            if (comp instanceof ImageOrbLabel) continue;
            if (comp instanceof JSpinner) continue;

            if (comp instanceof JPanel) {
                JPanel p = (JPanel) comp;
                if (Boolean.TRUE.equals(p.getClientProperty("tableRow"))) {
                    p.setBackground(cPanelAlt);
                    p.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(cBorder, 1),
                            new EmptyBorder(6, 12, 6, 12)));
                } else if (Boolean.TRUE.equals(p.getClientProperty("card"))) {
                    p.setBackground(cPanelAlt);
                    p.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(cBorder, 1),
                            new EmptyBorder(14, 14, 14, 14)));
                } else if (p.isOpaque() && p != sidebar) {
                    p.setBackground(cPanel);
                }
                applyThemeToTree(p);
            } else if (comp instanceof JLabel) {
                JLabel l = (JLabel) comp;
                if (Boolean.TRUE.equals(l.getClientProperty("rainbow"))) continue;
                if (Boolean.TRUE.equals(l.getClientProperty("customColor"))) continue;
                if (Boolean.TRUE.equals(l.getClientProperty("nextUpTitle"))) {
                    l.setForeground(cText);
                } else if (Boolean.TRUE.equals(l.getClientProperty("nextUpTime"))) {
                    l.setForeground(cAccentStripe);
                } else if (Boolean.TRUE.equals(l.getClientProperty("nextUpSubtext"))) {
                    l.setForeground(cSubtext);
                } else if (Boolean.TRUE.equals(l.getClientProperty("sidebarText"))) {
                    l.setForeground(cText);
                } else if (Boolean.TRUE.equals(l.getClientProperty("sidebarSubtext"))) {
                    l.setForeground(cSubtext);
                } else if (Boolean.TRUE.equals(l.getClientProperty("subtext"))) {
                    l.setForeground(cSubtext);
                } else {
                    l.setForeground(cText);
                }
            } else if (comp instanceof JTextField) {
                styleInput((JTextField) comp);
            } else if (comp instanceof JList) {
                JList<?> list = (JList<?>) comp;
                list.setBackground(darkMode ? new Color(28, 28, 34) : Color.WHITE);
                list.setForeground(cText);
                list.setSelectionBackground(darkMode ? new Color(70, 110, 180)
                        : new Color(190, 210, 240));
                list.setSelectionForeground(darkMode ? Color.WHITE : Color.BLACK);
            } else if (comp instanceof JComboBox) {
                JComboBox<?> cb = (JComboBox<?>) comp;
                cb.setForeground(cText);
            } else if (comp instanceof JRadioButton) {
                JRadioButton rb = (JRadioButton) comp;
                rb.setForeground(cText);
                rb.setOpaque(false);
            } else if (comp instanceof JScrollPane) {
                JScrollPane sp = (JScrollPane) comp;
                sp.setBackground(cPanel);
                sp.getViewport().setBackground(darkMode ? cBg : Color.WHITE);
                sp.setBorder(BorderFactory.createLineBorder(cBorder, 1));
                applyThemeToTree(sp);
            } else if (comp instanceof Container) {
                applyThemeToTree((Container) comp);
            }
        }
    }

    // ===== RAINBOW SIGNATURE =====
    private void startRainbow() {
        if (rainbowTimer != null) rainbowTimer.stop();
        rainbowTimer = new Timer(45, e -> {
            hue = (hue + 0.010f) % 1.0f;
            slivvyLabel.setForeground(Color.getHSBColor(hue, 0.85f, 1.0f));
        });
        rainbowTimer.start();
    }

    // ===== TIMER LOGIC =====
    private void startPresetTimer(String label, String message, long delay, TimeUnit unit) {
        long millis = unit.toMillis(delay);
        scheduleTimer(label, message, millis);
        setStatus("Started: " + label + " (" + formatDuration(millis) + ")");
    }

    private void startCustomTimer(CustomButtonDef def) {
        scheduleTimer(def.name, def.name + " is done! (" + formatDuration(def.durationMillis) + ")",
                def.durationMillis);
        setStatus("Started custom timer: " + def.name);
    }

    private void scheduleTimer(String label, String completionMessage, long delayMillis) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + delayMillis;
        ActiveTimer[] holder = new ActiveTimer[1];

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            if (holder[0] != null) {
                removeAndReportEnded(holder[0], true);
            }

            SwingUtilities.invokeLater(() -> {
                refreshActiveTimersList();
                if ("HOME".equals(currentScreen)) { statsDirty = true; refreshStatsUI(); }
                setStatus("Timer fired: " + label);

                playSound();
                showDesktopNotification("Timer Complete!", completionMessage);

                if (getExtendedState() == JFrame.ICONIFIED) setExtendedState(JFrame.NORMAL);
                if (isVisible()) {
                    toFront();
                    requestFocus();
                }

                JOptionPane.showMessageDialog(this, completionMessage,
                        "Timer Complete", JOptionPane.INFORMATION_MESSAGE);

                stopSound();
            });
        }, delayMillis, TimeUnit.MILLISECONDS);

        ActiveTimer timer = new ActiveTimer(label, startTime, endTime, future);
        holder[0] = timer;

        registerStart(timer);

        refreshActiveTimersList();
        if ("HOME".equals(currentScreen)) { statsDirty = true; refreshStatsUI(); }
    }

    private void registerStart(ActiveTimer t) {
        synchronized (statsLock) {
            activeTimers.add(t);
            TimerStats s = timerStats.computeIfAbsent(t.label, k -> new TimerStats());
            s.startedCount++;
            if (activityStartMillis == 0) {
                activityStartMillis = System.currentTimeMillis();
            }
            saveStats();
            saveActiveTimers();
        }
    }

    private String resolveRecipeKey(String label) {
        if (label == null) return null;
        String cleaned = label.trim();

        if (cleaned.regionMatches(true, 0, "Forge: ", 0, 7)) {
            cleaned = cleaned.substring(7).trim();
        }

        if (FORGE_RECIPES.containsKey(cleaned)) return cleaned;

        String normalised = cleaned.toUpperCase().replace(' ', '_');
        if (FORGE_RECIPES.containsKey(normalised)) return normalised;

        for (String key : FORGE_RECIPES.keySet()) {
            if (titleCase(key.toLowerCase()).equalsIgnoreCase(cleaned)) return key;
        }
        return null;
    }

    private void removeAndReportEnded(ActiveTimer t, boolean completed) {
        synchronized (statsLock) {
            activeTimers.remove(t);
            long elapsed = System.currentTimeMillis() - t.startTimeMillis;
            if (elapsed > 0) {
                timerDurations.merge(t.label, elapsed, Long::sum);
            }
            if (completed) {
                TimerStats s = timerStats.computeIfAbsent(t.label, k -> new TimerStats());
                s.completedCount++;

                String recipeKey = resolveRecipeKey(t.label);
                if (recipeKey != null) {
                    recordProfit(recipeKey);
                } else {
                    devLog("[profit] No recipe matched label \"" + t.label + "\"");
                }
            }
            if (activeTimers.isEmpty() && activityStartMillis > 0) {
                accumulatedActivityMillis += System.currentTimeMillis() - activityStartMillis;
                activityStartMillis = 0;
            }
            timerStatsDirty = true;
            saveStats();
            saveActiveTimers();
        }
    }

    // ===== ACTIVE TIMERS LIST =====
    private void refreshActiveTimersList() {
        long now = System.currentTimeMillis();

        if (activeTimersModel.size() == activeTimers.size()) {
            for (int i = 0; i < activeTimers.size(); i++) {
                ActiveTimer t = activeTimers.get(i);
                long remaining = t.endTimeMillis - now;
                String newText = "   " + t.label + "     \u2014     "
                        + formatDuration(remaining) + " remaining";
                if (!newText.equals(activeTimersModel.get(i))) {
                    activeTimersModel.set(i, newText);
                }
            }
            return;
        }

        ActiveTimer selectedTimer = null;
        int selIdx = activeTimersList.getSelectedIndex();
        if (selIdx >= 0 && selIdx < activeTimers.size()) {
            selectedTimer = activeTimers.get(selIdx);
        }

        activeTimersModel.clear();
        for (ActiveTimer t : activeTimers) {
            long remaining = t.endTimeMillis - now;
            activeTimersModel.addElement("   " + t.label + "     \u2014     "
                    + formatDuration(remaining) + " remaining");
        }

        if (selectedTimer != null) {
            int newIdx = activeTimers.indexOf(selectedTimer);
            if (newIdx >= 0) activeTimersList.setSelectedIndex(newIdx);
        }
    }

    // ===== CUSTOM SONG PLAYBACK =====
    private void playCustomSong(long token) {
        if (customSongPath == null || customSongPath.isEmpty()) {
            return;
        }
        final File file = new File(customSongPath);
        if (!file.isFile()) return;

        soundExecutor.submit(() -> {
            try {
                AudioFormat pcmFmt = determinePcmFormat(file);
                if (pcmFmt == null) return;

                try (SourceDataLine line = AudioSystem.getSourceDataLine(pcmFmt)) {
                    line.open(pcmFmt);
                    line.start();

                    final long deadline = System.currentTimeMillis() + SOUND_MAX_DURATION_MS;
                    final byte[] buf = new byte[8192];
                    final float vol = Math.max(0f, Math.min(1f, volume / 100f));

                    while (soundToken.get() == token
                            && System.currentTimeMillis() < deadline) {

                        try (AudioInputStream raw = AudioSystem.getAudioInputStream(file);
                             AudioInputStream pcm = toPcm16(raw, pcmFmt)) {

                            int n;
                            while (soundToken.get() == token
                                    && System.currentTimeMillis() < deadline
                                    && (n = pcm.read(buf)) > 0) {
                                if (vol < 0.999f) applyVolumeScaledToPcm16(buf, n, vol);
                                line.write(buf, 0, n);
                            }
                        }

                        if (soundToken.get() != token) break;
                        try { Thread.sleep(150); } catch (InterruptedException ie) { break; }
                    }
                    line.drain();
                }
            } catch (Exception ex) {
                try { Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}
            }
        });
    }

    private static AudioFormat determinePcmFormat(File f) {
        try (AudioInputStream src = AudioSystem.getAudioInputStream(f)) {
            AudioFormat sf = src.getFormat();
            float rate = sf.getSampleRate() > 0 ? sf.getSampleRate() : 44100f;
            int ch = Math.max(1, sf.getChannels());
            return new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    rate, 16, ch, ch * 2, rate, false);
        } catch (Exception ex) {
            return null;
        }
    }

    private static AudioInputStream toPcm16(AudioInputStream raw, AudioFormat target)
            throws Exception {
        AudioFormat sf = raw.getFormat();
        if (sf.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                && sf.getSampleSizeInBits() == 16
                && !sf.isBigEndian()) {
            return raw;
        }
        return AudioSystem.getAudioInputStream(target, raw);
    }

    private static void applyVolumeScaledToPcm16(byte[] buf, int len, float vol) {
        int limit = len & ~1;
        for (int i = 0; i < limit; i += 2) {
            int lo = buf[i] & 0xff;
            int hi = buf[i + 1];
            short s = (short) ((hi << 8) | lo);
            int scaled = (int) (s * vol);
            if (scaled > Short.MAX_VALUE)  scaled = Short.MAX_VALUE;
            if (scaled < Short.MIN_VALUE)  scaled = Short.MIN_VALUE;
            buf[i]     = (byte) (scaled & 0xff);
            buf[i + 1] = (byte) ((scaled >> 8) & 0xff);
        }
    }

    // ===== DESKTOP NOTIFICATIONS =====
    private void initSystemTray() {
        try {
            if (!SystemTray.isSupported()) { trayAvailable = false; return; }

            SystemTray tray = SystemTray.getSystemTray();

            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem("Show Timer Bot");
            showItem.addActionListener(e -> {
                setVisible(true);
                setExtendedState(getExtendedState() & ~JFrame.ICONIFIED);
                toFront();
                requestFocus();
            });

            MenuItem testItem = new MenuItem("Test Alert");
            testItem.addActionListener(e -> {
                playSound();
                showDesktopNotification("Test Alert",
                        "If you can see this, desktop notifications are working!");
            });

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                stopSound();
                if (rainbowTimer != null) rainbowTimer.stop();
                try { tray.remove(trayIcon); } catch (Exception ignored) {}
                scheduler.shutdownNow();
                soundExecutor.shutdownNow();
                dispose();
                System.exit(0);
            });

            popup.add(showItem);
            popup.add(testItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon = new TrayIcon(createAppIcon(), "Hypixel SkyBlock Timer", popup);
            trayIcon.setImageAutoSize(true);

            trayIcon.addActionListener(e -> {
                setVisible(true);
                setExtendedState(getExtendedState() & ~JFrame.ICONIFIED);
                toFront();
                requestFocus();
            });

            tray.add(trayIcon);
            trayAvailable = true;
        } catch (Exception ex) {
            trayAvailable = false;
            trayIcon = null;
        }
    }

    private void showDesktopNotification(String title, String body) {
        if (!trayAvailable || trayIcon == null) return;
        try {
            trayIcon.displayMessage(title, body, TrayIcon.MessageType.INFO);
        } catch (Exception ignored) {
        }
    }

    // ===== SOUND LIBRARY =====
    private static final long SOUND_MAX_DURATION_MS = 60_000;

    private void playSound() {
        if (soundChoice == null || soundChoice.equals("None")) return;

        final long myToken = soundToken.incrementAndGet();

        if ("Custom Song".equals(soundChoice)) {
            playCustomSong(myToken);
            return;
        }

        final double[][] tones = tonesFor(soundChoice);
        final Waveform wf = waveformFor(soundChoice);
        final long gapMs = loopGapFor(soundChoice);
        if (tones.length == 0) return;

        soundExecutor.submit(() -> {
            long deadline = System.currentTimeMillis() + SOUND_MAX_DURATION_MS;
            try {
                while (soundToken.get() == myToken
                        && System.currentTimeMillis() < deadline) {
                    boolean completed = playSequence(tones, wf, myToken);
                    if (!completed) break;
                    Thread.sleep(gapMs);
                }
            } catch (InterruptedException ignored) {
            } catch (Exception ex) {
                try { Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}
            }
        });
    }

    private void stopSound() {
        soundToken.incrementAndGet();
    }

    private double[][] tonesFor(String choice) {
        return switch (choice) {
        case "Dopamine" -> new double[][]{
            {1046.50, 70},
            {1567.98, 130},
            {523.25, 55},
            {659.25, 55},
            {783.99, 55},
            {1046.50, 55},
            {1318.51, 55},
            {1567.98, 55},
            {2093.00, 320},
            {2637.02, 55},
            {2093.00, 55},
            {2637.02, 420},
    };
            case "Beep" -> new double[][]{ {880, 150} };
            case "Chime" -> new double[][]{
                    {523.25, 220}, {659.25, 220}, {784.00, 500} };
            case "Bell" -> new double[][]{
                    {1318.51, 700}, {1046.50, 900} };
            case "Coin" -> new double[][]{
                    {987.77, 80}, {1318.51, 650} };
            case "Level Up" -> new double[][]{
                    {523.25, 90}, {587.33, 90}, {659.25, 90}, {698.46, 90},
                    {783.99, 90}, {880.00, 90}, {987.77, 90}, {1046.50, 350} };
            case "Anvil" -> new double[][]{
                    {2400, 60}, {1800, 80}, {2600, 60}, {1500, 100} };
            case "Villager" -> new double[][]{
                    {350, 120}, {300, 120}, {350, 180} };
            case "Alarm" -> new double[][]{
                    {880, 130}, {0, 70}, {880, 130}, {0, 70},
                    {880, 130}, {0, 70}, {880, 130}, {0, 400} };
            case "Siren" -> buildSweep(new double[][]{
                    {400, 1400, 30, 25}, {1400, 400, 30, 25} });
            case "Portal" -> buildSweep(new double[][]{
                    {180, 800, 22, 40}, {800, 180, 22, 40} });
            default -> new double[0][];
        };
    }

    private Waveform waveformFor(String choice) {
        return switch (choice) {
        case "Chime", "Bell", "Siren", "Portal", "Dopamine" -> Waveform.SINE;
            default -> Waveform.SQUARE;
        };
    }

    private long loopGapFor(String choice) {
        return switch (choice) {
            case "Beep" -> 400;
            case "Chime" -> 400;
            case "Bell" -> 400;
            case "Coin" -> 300;
            case "Level Up" -> 500;
            case "Anvil" -> 600;
            case "Villager" -> 400;
            case "Alarm" -> 300;
            case "Dopamine" -> 700;
            case "Siren" -> 200;
            case "Portal" -> 400;
            default -> 500;
        };
    }

    private double[][] buildSweep(double[][] sweeps) {
        List<double[]> out = new ArrayList<>();
        for (double[] s : sweeps) {
            double from = s[0], to = s[1];
            int steps = (int) s[2];
            double ms = s[3];
            for (int i = 0; i < steps; i++) {
                double f = from + (to - from) * ((double) i / (steps - 1));
                out.add(new double[]{f, ms});
            }
        }
        return out.toArray(new double[0][]);
    }

    private boolean playSequence(double[][] tones, Waveform wf, long token) throws Exception {
        float rate = 44100f;
        int totalSamples = 0;
        for (double[] t : tones) totalSamples += (int) (rate * t[1] / 1000.0);
        if (totalSamples == 0) return false;

        byte[] buf = new byte[totalSamples * 2];
        int offset = 0;
        for (double[] t : tones) {
            int samples = (int) (rate * t[1] / 1000.0);
            double freq = t[0];
            for (int i = 0; i < samples; i++) {
                double sample;
                if (freq <= 0) {
                    sample = 0;
                } else {
                    double angle = 2.0 * Math.PI * freq * i / rate;
                    sample = (wf == Waveform.SQUARE)
                            ? (Math.sin(angle) >= 0 ? 1.0 : -1.0)
                            : Math.sin(angle);
                }

                double progress = (double) i / samples;
                double env = 1.0;
                if (progress < 0.05) env = progress / 0.05;
                else if (progress > 0.90) env = (1.0 - progress) / 0.10;

                double vol = Math.max(0.0, Math.min(1.0, volume / 100.0));
                short val = (short) (sample * Short.MAX_VALUE * 0.5 * env * vol);
                buf[(offset + i) * 2]     = (byte) (val & 0xff);
                buf[(offset + i) * 2 + 1] = (byte) ((val >> 8) & 0xff);
            }
            offset += samples;
        }

        AudioFormat format = new AudioFormat(rate, 16, 1, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();

            int written = 0;
            final int chunk = 8192;
            while (written < buf.length) {
                if (soundToken.get() != token) {
                    line.flush();
                    line.stop();
                    return false;
                }
                int toWrite = Math.min(chunk, buf.length - written);
                line.write(buf, written, toWrite);
                written += toWrite;
            }
            line.drain();
        }
        return true;
    }

    // ===== CUSTOM BUTTON PERSISTENCE =====
    private void loadCustomButtons() {
        customButtons.clear();
        int count = prefs.getInt("custom.count", 0);
        for (int i = 0; i < count; i++) {
            String name = prefs.get("custom." + i + ".name", null);
            long dur = prefs.getLong("custom." + i + ".duration", 0);
            if (name != null && dur > 0) customButtons.add(new CustomButtonDef(name, dur));
        }
    }

    private void saveCustomButtons() {
        prefs.putInt("custom.count", customButtons.size());
        for (int i = 0; i < customButtons.size(); i++) {
            prefs.put("custom." + i + ".name", customButtons.get(i).name);
            prefs.putLong("custom." + i + ".duration", customButtons.get(i).durationMillis);
        }
        for (int i = customButtons.size(); i < 200; i++) {
            if (prefs.get("custom." + i + ".name", null) == null) break;
            prefs.remove("custom." + i + ".name");
            prefs.remove("custom." + i + ".duration");
        }
    }

    // ===== STATS PERSISTENCE =====
    private void loadStats() {
        timerStats.clear();
        timerDurations.clear();
        accumulatedActivityMillis = prefs.getLong("stats.activity.accumulated", 0);
        activityStartMillis = 0;

        String prefix = "stats.timer.";
        try {
            for (String key : prefs.keys()) {
                if (!key.startsWith(prefix)) continue;
                if (key.endsWith(".started")) {
                    String enc = key.substring(prefix.length(), key.length() - ".started".length());
                    String label = URLDecoder.decode(enc, StandardCharsets.UTF_8);
                    int started = prefs.getInt(key, 0);
                    int completed = prefs.getInt(prefix + enc + ".completed", 0);
                    if (started > 0 || completed > 0) {
                        timerStats.put(label, new TimerStats(started, completed));
                    }
                } else if (key.endsWith(".duration")) {
                    String enc = key.substring(prefix.length(), key.length() - ".duration".length());
                    String label = URLDecoder.decode(enc, StandardCharsets.UTF_8);
                    long dur = prefs.getLong(key, 0);
                    if (dur > 0) timerDurations.put(label, dur);
                }
            }
        } catch (BackingStoreException ignored) {}
    }

    private void saveActiveTimers() {
        synchronized (statsLock) {
            prefs.putInt("active.count", activeTimers.size());
            int i = 0;
            for (ActiveTimer t : activeTimers) {
                prefs.put("active." + i + ".label", t.label);
                prefs.putLong("active." + i + ".start", t.startTimeMillis);
                prefs.putLong("active." + i + ".end",   t.endTimeMillis);
                i++;
            }
            for (int j = i; j < 100; j++) {
                if (prefs.get("active." + j + ".label", null) == null) break;
                prefs.remove("active." + j + ".label");
                prefs.remove("active." + j + ".start");
                prefs.remove("active." + j + ".end");
            }
        }
    }

    private void restoreActiveTimers() {
        int count = prefs.getInt("active.count", 0);
        if (count == 0) return;

        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            String label = prefs.get("active." + i + ".label", null);
            long start = prefs.getLong("active." + i + ".start", 0);
            long end   = prefs.getLong("active." + i + ".end",   0);
            if (label == null || start <= 0 || end <= 0) continue;

            long remaining = end - now;
            if (remaining <= 0) {
                long elapsed = end - start;
                if (elapsed > 0) timerDurations.merge(label, elapsed, Long::sum);
                TimerStats s = timerStats.computeIfAbsent(label, k -> new TimerStats());
                s.completedCount++;

                String recipeKey = resolveRecipeKey(label);
                if (recipeKey != null) {
                    recordProfit(recipeKey);
                }
                continue;
            }

            ActiveTimer[] holder = new ActiveTimer[1];
            final String lbl = label;
            ScheduledFuture<?> future = scheduler.schedule(() -> {
                if (holder[0] != null) {
                    removeAndReportEnded(holder[0], true);
                }
                SwingUtilities.invokeLater(() -> {
                    refreshActiveTimersList();
                    if ("HOME".equals(currentScreen)) { statsDirty = true; refreshStatsUI(); }
                    setStatus("Timer fired: " + lbl);
                    playSound();
                    showDesktopNotification("Timer Complete!", lbl + " is complete!");
                    if (getExtendedState() == JFrame.ICONIFIED) setExtendedState(JFrame.NORMAL);
                    if (isVisible()) {
                        toFront();
                        requestFocus();
                    }
                    JOptionPane.showMessageDialog(this, lbl + " is complete!",
                            "Timer Complete", JOptionPane.INFORMATION_MESSAGE);
                    stopSound();
                });
            }, remaining, TimeUnit.MILLISECONDS);

            ActiveTimer t = new ActiveTimer(label, start, end, future);
            holder[0] = t;
            activeTimers.add(t);
        }

        if (activityStartMillis == 0 && !activeTimers.isEmpty()) {
            activityStartMillis = System.currentTimeMillis();
        }

        saveStats();
        saveActiveTimers();
    }

    private void resetStats() {
        synchronized (statsLock) {
            try {
                for (String key : prefs.keys()) {
                    if (key.startsWith("stats.")) prefs.remove(key);
                }
            } catch (BackingStoreException ignored) {}
            timerStats.clear();
            timerDurations.clear();
            accumulatedActivityMillis = 0;
            activityStartMillis = activeTimers.isEmpty() ? 0 : System.currentTimeMillis();
            saveStats();
        }
        statsDirty = true;
        timerStatsDirty = true;
        refreshStatsUI();
    }

    // ===== HTTP + HYPIXEL API =====
    private static CompletableFuture<String> httpGet(String url, String apiKeyHeader) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "HypixelTimerApp/1.0")
                .GET();
        if (apiKeyHeader != null && !apiKeyHeader.isEmpty()) {
            b.header("API-Key", apiKeyHeader);
        }
        return HTTP.sendAsync(b.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        throw new RuntimeException("HTTP " + resp.statusCode()
                                + " from " + url + "\n" + truncate(resp.body(), 400));
                    }
                    return resp.body();
                });
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static CompletableFuture<String> fetchUuid(String username) {
        if (username.matches("[0-9a-fA-F]{32}")) {
            return CompletableFuture.completedFuture(username.toLowerCase());
        }
        String url = "https://api.mojang.com/users/profiles/minecraft/" + username;
        return httpGet(url, null).thenApply(body -> {
            Object parsed = Json.parse(body);
            String id = Json.str(Json.getPath(parsed, "id"));
            if (id == null || id.isEmpty()) {
                throw new RuntimeException("Player not found: " + username);
            }
            return id;
        });
    }

    private static CompletableFuture<Object> fetchPlayer(String uuid, String apiKey) {
        String url = "https://api.hypixel.net/v2/player?uuid=" + uuid;
        return httpGet(url, apiKey).thenApply(Json::parse);
    }

    private static CompletableFuture<Object> fetchSkyblockProfiles(String uuid, String apiKey) {
        String url = "https://api.hypixel.net/v2/skyblock/profiles?uuid=" + uuid;
        return httpGet(url, apiKey).thenApply(Json::parse);
    }

    // ===== MINIMAL JSON PARSER =====
    private static class Json {
        static Object parse(String s) {
            if (s == null) return null;
            try {
                Parser p = new Parser(s);
                return p.parseValue();
            } catch (Exception e) {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        static Object getPath(Object obj, String... path) {
            Object cur = obj;
            for (String p : path) {
                if (cur == null) return null;
                if (cur instanceof Map) {
                    cur = ((Map<String, Object>) cur).get(p);
                } else if (cur instanceof List) {
                    try {
                        int idx = Integer.parseInt(p);
                        List<Object> list = (List<Object>) cur;
                        cur = (idx >= 0 && idx < list.size()) ? list.get(idx) : null;
                    } catch (NumberFormatException e) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
            return cur;
        }

        static String str(Object o) {
            if (o == null) return null;
            return o instanceof String ? (String) o : String.valueOf(o);
        }

        static long lng(Object o) {
            return (o instanceof Number) ? ((Number) o).longValue() : 0L;
        }

        static int integer(Object o) {
            return (o instanceof Number) ? ((Number) o).intValue() : 0;
        }

        static double dbl(Object o) {
            return (o instanceof Number) ? ((Number) o).doubleValue() : 0.0;
        }

        private static class Parser {
            final String s;
            int i;
            Parser(String s) { this.s = s; }

            Object parseValue() {
                skipWs();
                if (i >= s.length()) return null;
                char c = s.charAt(i);
                if (c == '{') return parseObject();
                if (c == '[') return parseArray();
                if (c == '"') return parseString();
                if (c == 't') { i += 4; return Boolean.TRUE; }
                if (c == 'f') { i += 5; return Boolean.FALSE; }
                if (c == 'n') { i += 4; return null; }
                return parseNumber();
            }

            Map<String, Object> parseObject() {
                Map<String, Object> map = new LinkedHashMap<>();
                i++;
                skipWs();
                if (i < s.length() && s.charAt(i) == '}') { i++; return map; }
                while (i < s.length()) {
                    skipWs();
                    if (s.charAt(i) != '"') break;
                    String key = parseString();
                    skipWs();
                    if (i < s.length() && s.charAt(i) == ':') i++;
                    Object val = parseValue();
                    map.put(key, val);
                    skipWs();
                    if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
                    if (i < s.length() && s.charAt(i) == '}') { i++; break; }
                    break;
                }
                return map;
            }

            List<Object> parseArray() {
                List<Object> list = new ArrayList<>();
                i++;
                skipWs();
                if (i < s.length() && s.charAt(i) == ']') { i++; return list; }
                while (i < s.length()) {
                    list.add(parseValue());
                    skipWs();
                    if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
                    if (i < s.length() && s.charAt(i) == ']') { i++; break; }
                    break;
                }
                return list;
            }

            String parseString() {
                skipWs();
                if (i >= s.length() || s.charAt(i) != '"') return null;
                i++;
                StringBuilder sb = new StringBuilder();
                while (i < s.length()) {
                    char c = s.charAt(i++);
                    if (c == '"') break;
                    if (c == '\\' && i < s.length()) {
                        char e = s.charAt(i++);
                        switch (e) {
                            case 'n': sb.append('\n'); break;
                            case 't': sb.append('\t'); break;
                            case 'r': sb.append('\r'); break;
                            case 'b': sb.append('\b'); break;
                            case 'f': sb.append('\f'); break;
                            case '/': sb.append('/'); break;
                            case '\\': sb.append('\\'); break;
                            case '"': sb.append('"'); break;
                            case 'u':
                                if (i + 4 <= s.length()) {
                                    String hex = s.substring(i, i + 4);
                                    try {
                                        sb.append((char) Integer.parseInt(hex, 16));
                                    } catch (NumberFormatException ignored) {}
                                    i += 4;
                                }
                                break;
                            default: sb.append(e);
                        }
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            }

            Object parseNumber() {
                int start = i;
                while (i < s.length()) {
                    char c = s.charAt(i);
                    if (Character.isDigit(c) || c == '-' || c == '+' || c == '.'
                            || c == 'e' || c == 'E') i++;
                    else break;
                }
                String num = s.substring(start, i);
                if (num.isEmpty()) return null;
                try {
                    if (num.indexOf('.') >= 0 || num.indexOf('e') >= 0 || num.indexOf('E') >= 0) {
                        return Double.parseDouble(num);
                    }
                    return Long.parseLong(num);
                } catch (NumberFormatException e) {
                    return null;
                }
            }

            void skipWs() {
                while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            }
        }
    }

    // ===== HELPERS =====
    private void setStatus(String msg) {
        statusBar.setText("  " + msg);
    }

    // ===== APP ICON =====
    private static Image createAppIcon() {
        final int SRC   = 16;
        final int SCALE = 4;
        final int SIZE  = SRC * SCALE;

        final int TRANSPARENT = 0x00000000;
        final int GOLD_DARK   = 0xFF7a5010;
        final int GOLD_MID    = 0xFFc89020;
        final int GOLD_LITE   = 0xFFe8c860;
        final int FACE_MAIN   = 0xFFf8f0d8;
        final int FACE_SHADE  = 0xFFd8cfa8;
        final int HAND_COL    = 0xFF3a2010;
        final int HUB_COL     = 0xFFc42828;

        BufferedImage small = new BufferedImage(SRC, SRC, BufferedImage.TYPE_INT_ARGB);

        final double cx = 7.5;
        final double cy = 7.5;

        for (int y = 0; y < SRC; y++) {
            for (int x = 0; x < SRC; x++) {
                double dx = x - cx;
                double dy = y - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);

                int col = TRANSPARENT;
                if (dist <= 5.0) {
                    col = ((dx + dy) > 2.5) ? FACE_SHADE : FACE_MAIN;
                } else if (dist <= 6.5) {
                    if ((dx + dy) < -1.5)      col = GOLD_LITE;
                    else if ((dx + dy) > 1.5)  col = GOLD_DARK;
                    else                       col = GOLD_MID;
                }
                small.setRGB(x, y, col);
            }
        }

        for (int y = 3; y <= 6; y++) small.setRGB(7, y, HAND_COL);
        for (int x = 8; x <= 10; x++) small.setRGB(x, 7, HAND_COL);
        small.setRGB(7, 7, HUB_COL);

        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D og = out.createGraphics();
        og.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        og.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        og.drawImage(small, 0, 0, SIZE, SIZE, null);
        og.dispose();

        return out;
    }

    private void installModernScrollbar(JScrollPane scroll) {
        if (scroll == null) return;
        if (!themedScrollPanes.contains(scroll)) themedScrollPanes.add(scroll);
        applyScrollbarTheme(scroll);
    }

    private void applyScrollbarTheme(JScrollPane scroll) {
        if (scroll == null) return;
        final boolean dark = darkMode;
        final Color thumbIdle  = dark ? new Color(82, 84, 98, 200)   : new Color(168, 172, 184, 200);
        final Color thumbHover = dark ? new Color(120, 124, 140, 230) : new Color(140, 145, 160, 230);
        final Color thumbPress = new Color(60, 130, 220);

        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JScrollBar sb = scroll.getVerticalScrollBar();
        sb.setPreferredSize(new Dimension(10, 0));
        sb.setOpaque(false);
        sb.setUnitIncrement(16);
        sb.setBlockIncrement(80);
        sb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                this.thumbColor = thumbIdle;
                this.trackColor = new Color(0, 0, 0, 0);
            }
            @Override protected JButton createDecreaseButton(int o) { return zero(); }
            @Override protected JButton createIncreaseButton(int o) { return zero(); }
            private JButton zero() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (r.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color col = thumbIdle;
                if (isThumbRollover()) col = thumbHover;
                if (isDragging)        col = thumbPress;
                g2.setColor(col);
                g2.fillRoundRect(r.x + 3, r.y + 2, r.width - 6, r.height - 4, 8, 8);
                g2.dispose();
            }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
        });
        sb.repaint();
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT_UI, Font.BOLD, 15));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static String formatDuration(long millis) {
        if (millis < 0) millis = 0;
        long totalSeconds = millis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    private static class ScrollablePanel extends JPanel implements Scrollable {
        ScrollablePanel() { super(); }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }
        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 100;
        }
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // ===== MODERN COMBO BOX UI =====
    private static class ModernComboBoxUI extends javax.swing.plaf.basic.BasicComboBoxUI {
        private final HypixelTimerApp owner;
        ModernComboBoxUI(HypixelTimerApp owner) { this.owner = owner; }

        @Override public void installUI(JComponent c) {
            super.installUI(c);
            c.setOpaque(false);
            c.setBorder(new EmptyBorder(0, 0, 0, 0));
        }

        @Override protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    Color arrowCol = owner.darkMode
                            ? (getModel().isRollover() ? new Color(230, 230, 245) : new Color(180, 180, 195))
                            : (getModel().isRollover() ? new Color(20, 20, 30)    : new Color(90, 90, 100));
                    g2.setColor(arrowCol);
                    int w = 9, h = 5;
                    Path2D tri = new Path2D.Float();
                    tri.moveTo(cx - w / 2.0, cy - h / 2.0);
                    tri.lineTo(cx + w / 2.0, cy - h / 2.0);
                    tri.lineTo(cx,           cy + h / 2.0);
                    tri.closePath();
                    g2.fill(tri);
                    g2.dispose();
                }
            };
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setOpaque(false);
            b.setPreferredSize(new Dimension(26, 0));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        }

        @Override
        protected javax.swing.plaf.basic.ComboPopup createPopup() {
            javax.swing.plaf.basic.ComboPopup p = super.createPopup();
            JList<?> list = p.getList();
            if (list != null) {
                list.setBackground(owner.darkMode ? new Color(28, 29, 38) : Color.WHITE);
                list.setSelectionBackground(owner.darkMode ? new Color(58, 90, 140) : new Color(200, 216, 240));
                list.setForeground(owner.darkMode ? new Color(238, 238, 244) : new Color(24, 24, 30));
                list.setSelectionForeground(owner.darkMode ? Color.WHITE : new Color(20, 40, 80));
            }
            return p;
        }

        @Override
        protected ListCellRenderer<Object> createRenderer() {
            return new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value,
                                                              int index, boolean isSelected,
                                                              boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    setFont(new Font(FONT_UI, Font.PLAIN, 13));

                    if (index < 0) {
                        setOpaque(false);
                        setBorder(new EmptyBorder(0, 0, 0, 0));
                        setForeground(owner.darkMode ? new Color(238, 238, 244) : new Color(24, 24, 30));
                    } else {
                        setOpaque(true);
                        setBorder(new EmptyBorder(6, 12, 6, 12));
                        if (isSelected) {
                            setBackground(owner.darkMode ? new Color(58, 90, 140) : new Color(200, 216, 240));
                            setForeground(owner.darkMode ? Color.WHITE : new Color(20, 40, 80));
                        } else {
                            setBackground(owner.darkMode ? new Color(28, 29, 38) : Color.WHITE);
                            setForeground(owner.darkMode ? new Color(238, 238, 244) : new Color(24, 24, 30));
                        }
                    }
                    return this;
                }
            };
        }

        void applyTheme() {
            if (popup == null) return;
            JList<?> list = popup.getList();
            if (list == null) return;
            list.setBackground(owner.darkMode ? new Color(28, 29, 38) : Color.WHITE);
            list.setSelectionBackground(owner.darkMode ? new Color(58, 90, 140) : new Color(200, 216, 240));
            list.setForeground(owner.darkMode ? new Color(238, 238, 244) : new Color(24, 24, 30));
            list.setSelectionForeground(owner.darkMode ? Color.WHITE : new Color(20, 40, 80));
            list.repaint();
        }
    }

    // ===== MODERN SPINNER UI =====
    private static class ModernSpinnerUI extends javax.swing.plaf.basic.BasicSpinnerUI {
        private final HypixelTimerApp owner;
        ModernSpinnerUI(HypixelTimerApp owner) { this.owner = owner; }

        @Override protected Component createNextButton() {
            return makeArrowButton(true);
        }
        @Override protected Component createPreviousButton() {
            return makeArrowButton(false);
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            spinner.setOpaque(false);
            spinner.setBackground(new Color(0, 0, 0, 0));
            spinner.setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        protected void installListeners() {
            super.installListeners();
            if (spinner.getEditor() instanceof JSpinner.DefaultEditor) {
                JSpinner.DefaultEditor ed = (JSpinner.DefaultEditor) spinner.getEditor();
                ed.setOpaque(false);
                ed.setBackground(new Color(0, 0, 0, 0));
                JTextField tf = ed.getTextField();
                tf.setOpaque(false);
                tf.setBackground(new Color(0, 0, 0, 0));
                tf.setBorder(BorderFactory.createEmptyBorder());
            }
        }

        private JButton makeArrowButton(boolean up) {
            JButton b = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    Color base = owner.darkMode ? new Color(170, 170, 185) : new Color(90, 90, 100);
                    if (getModel().isRollover()) base = base.brighter();
                    g2.setColor(base);
                    int w = 7, h = 4;
                    Path2D tri = new Path2D.Float();
                    if (up) {
                        tri.moveTo(cx - w / 2.0, cy + h / 2.0);
                        tri.lineTo(cx + w / 2.0, cy + h / 2.0);
                        tri.lineTo(cx,           cy - h / 2.0);
                    } else {
                        tri.moveTo(cx - w / 2.0, cy - h / 2.0);
                        tri.lineTo(cx + w / 2.0, cy - h / 2.0);
                        tri.lineTo(cx,           cy + h / 2.0);
                    }
                    tri.closePath();
                    g2.fill(tri);
                    g2.dispose();
                }
            };
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setOpaque(false);
            b.setPreferredSize(new Dimension(20, 0));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            b.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (spinner == null) return;
                    SpinnerModel model = spinner.getModel();
                    Object next = up ? model.getNextValue() : model.getPreviousValue();
                    if (next != null) {
                        model.setValue(next);
                    }
                }
            });

            return b;
        }
    }

    // ===== VECTOR ICON =====
    private static class NavIcon implements Icon {
        private final NavIconType type;
        private final int size;

        NavIcon(NavIconType type) { this(type, 18); }
        NavIcon(NavIconType type, int size) {
            this.type = type;
            this.size = size;
        }

        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getForeground());
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int s = size;
            int cx = x + s / 2;
            int cy = y + s / 2;
            int r = s / 2 - 1;

            switch (type) {
                case HOME -> {
                    g2.drawLine(cx - r, cy, cx, cy - r);
                    g2.drawLine(cx, cy - r, cx + r, cy);
                    g2.drawLine(cx - r + 2, cy, cx - r + 2, cy + r);
                    g2.drawLine(cx + r - 2, cy, cx + r - 2, cy + r);
                    g2.drawLine(cx - r + 2, cy + r, cx + r - 2, cy + r);
                    g2.drawLine(cx - 2, cy + r, cx - 2, cy + r - 3);
                    g2.drawLine(cx + 2, cy + r, cx + 2, cy + r - 3);
                    g2.drawLine(cx - 2, cy + r - 3, cx + 2, cy + r - 3);
                }
                case HELP -> {
                    g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                    g2.fillOval(cx - 1, cy - r + 3, 3, 3);
                    g2.drawLine(cx - 2, cy - r + 5, cx + 2, cy - r + 5);
                    g2.drawArc(cx - 3, cy - 3, 6, 6, 30, 180);
                    g2.fillOval(cx - 1, cy + r - 4, 3, 3);
                }
                case FORGE -> {
                    g2.fillRect(cx - r + 1, cy - r + 2, r * 2 - 2, 5);
                    g2.drawLine(cx, cy - r + 7, cx, cy + r);
                }
                case KAT -> {
                    g2.fillOval(cx - 5, cy + 1, 10, 8);
                    g2.fillOval(cx - 8, cy - 5, 5, 5);
                    g2.fillOval(cx - 2, cy - 8, 5, 5);
                    g2.fillOval(cx + 3, cy - 5, 5, 5);
                }
                case CUSTOM -> {
                    Path2D star = new Path2D.Float();
                    int points = 5;
                    int outerR = r;
                    int innerR = r / 2 + 1;
                    for (int i = 0; i < points * 2; i++) {
                        double angle = Math.PI * i / points - Math.PI / 2;
                        int rr = (i % 2 == 0) ? outerR : innerR;
                        int px = cx + (int) (Math.cos(angle) * rr);
                        int py = cy + (int) (Math.sin(angle) * rr);
                        if (i == 0) star.moveTo(px, py);
                        else star.lineTo(px, py);
                    }
                    star.closePath();
                    g2.draw(star);
                }
                case ACTIVE -> {
                    g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                    g2.drawLine(cx, cy, cx, cy - r + 2);
                    g2.drawLine(cx, cy, cx + r - 3, cy);
                }
                case STATS -> {
                    int baseY = cy + r;
                    int barW  = Math.max(2, s / 6);
                    int gap   = Math.max(1, (s - barW * 3) / 4);
                    int x0    = cx - (barW * 3 + gap * 2) / 2;
                    int h1 = r / 2;
                    int h2 = (int) (r * 0.8);
                    int h3 = r + 1;
                    g2.fillRect(x0,                    baseY - h1, barW, h1);
                    g2.fillRect(x0 + barW + gap,       baseY - h2, barW, h2);
                    g2.fillRect(x0 + (barW + gap) * 2, baseY - h3, barW, h3);
                    g2.drawLine(cx - r, baseY, cx + r, baseY);
                }
                case PROFITS -> {
                    g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                    g2.setStroke(new BasicStroke(1.5f));
                    int inner = r - 4;
                    g2.drawOval(cx - inner, cy - inner, inner * 2, inner * 2);
                    g2.fillOval(cx - 2, cy - 2, 4, 4);
                }
                case SETTINGS -> {
                    int innerR = r - 4;
                    g2.drawOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4;
                        int x1 = cx + (int) (Math.cos(angle) * (innerR - 1));
                        int y1 = cy + (int) (Math.sin(angle) * (innerR - 1));
                        int x2 = cx + (int) (Math.cos(angle) * r);
                        int y2 = cy + (int) (Math.sin(angle) * r);
                        g2.drawLine(x1, y1, x2, y2);
                    }
                }
                case TIMER -> {
                    int innerR = r - 2;
                    g2.drawOval(cx - innerR, cy - innerR + 1, innerR * 2, innerR * 2 - 1);
                    g2.drawLine(cx - 2, cy - innerR, cx + 2, cy - innerR);
                    g2.drawLine(cx - 2, cy - innerR - 2, cx + 2, cy - innerR - 2);
                    g2.drawLine(cx - 2, cy - innerR - 2, cx - 2, cy - innerR);
                    g2.drawLine(cx + 2, cy - innerR - 2, cx + 2, cy - innerR);
                    g2.drawLine(cx, cy + 1, cx, cy - innerR + 3);
                    g2.drawLine(cx, cy + 1, cx + innerR - 3, cy + 1);
                    g2.fillOval(cx - 1, cy, 3, 3);
                }
            }
            g2.dispose();
        }
    }

    // ===== TAB BUTTON =====
    private static class TabButton extends JButton {
        private boolean active;
        private final HypixelTimerApp owner;

        TabButton(String text, boolean active, HypixelTimerApp owner) {
            super(text);
            this.active = active;
            this.owner = owner;
            owner.tabButtons.add(this);

            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setFont(new Font(FONT_UI, Font.BOLD, 13));
            setMargin(new Insets(10, 22, 10, 22));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setActive(boolean a) { this.active = a; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            boolean dark = owner.darkMode;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (active) {
                g2.setColor(new Color(60, 130, 220));
            } else if (getModel().isRollover()) {
                g2.setColor(dark ? new Color(56, 56, 66) : new Color(226, 228, 234));
            } else {
                g2.setColor(dark ? new Color(42, 42, 52) : new Color(232, 234, 240));
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();

            setForeground(active ? Color.WHITE
                                 : (dark ? new Color(220, 220, 230) : new Color(50, 50, 60)));
            super.paintComponent(g);
        }
    }

    // ===== NAV BUTTON =====
    private static class NavButton extends JButton {
        final String key;
        private boolean selected;
        private boolean dark;

        private float selectProgress = 0f;
        private float hoverProgress  = 0f;

        NavButton(NavIconType iconType, String text, String key, HypixelTimerApp owner, Runnable onClick) {
            super(text);
            this.key = key;
            this.dark = owner.darkMode;
            owner.navButtons.add(this);

            setIcon(new NavIcon(iconType));
            setIconTextGap(12);
            setHorizontalAlignment(SwingConstants.LEFT);
            setHorizontalTextPosition(SwingConstants.RIGHT);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setFont(new Font(FONT_UI, Font.PLAIN, 14));
            setMargin(new Insets(10, 14, 10, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(new EmptyBorder(10, 14, 10, 14));

            addActionListener(e -> onClick.run());
        }

        void setNavSelected(boolean s) { this.selected = s; }
        void setDark(boolean d) { this.dark = d; repaint(); }

        void tickAnimation() {
            float targetSel = selected ? 1f : 0f;
            float deltaSel = targetSel - selectProgress;
            if (Math.abs(deltaSel) < 0.002f) {
                selectProgress = targetSel;
            } else {
                selectProgress += deltaSel * 0.18f;
                repaint();
            }

            float targetHov = getModel().isRollover() ? 1f : 0f;
            float deltaHov = targetHov - hoverProgress;
            if (Math.abs(deltaHov) < 0.002f) {
                hoverProgress = targetHov;
            } else {
                hoverProgress += deltaHov * 0.22f;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 10;

            float bgStrength = Math.max(selectProgress, hoverProgress * 0.55f);
            if (bgStrength > 0.01f) {
                Color base;
                if (selected || selectProgress > 0.5f) {
                    base = dark ? new Color(58, 90, 140) : new Color(200, 216, 240);
                } else {
                    base = dark ? new Color(46, 46, 56) : new Color(224, 228, 236);
                }
                int alpha = (int) (255 * bgStrength);
                alpha = Math.max(0, Math.min(255, alpha));
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
            }

            if (selectProgress > 0.01f) {
                int barHeight = (int) (h * 0.72f * selectProgress);
                int barY = (h - barHeight) / 2;
                int barW = Math.max(3, (int) (4 * selectProgress));

                Color accent = new Color(60, 130, 220);
                int glowAlpha = (int) (90 * selectProgress);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), glowAlpha));
                g2.fillRoundRect(0, barY - 2, barW + 4, barHeight + 4, barW + 4, barW + 4);

                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                        (int) (255 * selectProgress)));
                g2.fillRoundRect(1, barY, barW, barHeight, barW, barW);
            }

            g2.dispose();

            Color selectedColor = dark ? Color.WHITE : new Color(20, 40, 80);
            Color idleColor     = dark ? new Color(200, 200, 210) : new Color(60, 60, 72);
            float t = Math.max(selectProgress, hoverProgress * 0.4f);
            int r = (int) (idleColor.getRed()   * (1 - t) + selectedColor.getRed()   * t);
            int gN= (int) (idleColor.getGreen() * (1 - t) + selectedColor.getGreen() * t);
            int b = (int) (idleColor.getBlue()  * (1 - t) + selectedColor.getBlue()  * t);
            setForeground(new Color(r, gN, b));

            super.paintComponent(g);
        }
    }

    // ===== STAT CARD =====
    private static class StatCard extends JPanel {
        final JLabel valueLabel;
        private final JLabel titleLabel;

        StatCard(String title, String value, HypixelTimerApp owner) {
            setLayout(new BorderLayout());
            setOpaque(true);
            setBorder(new EmptyBorder(14, 18, 14, 18));

            titleLabel = new JLabel(title);
            titleLabel.setFont(new Font(FONT_UI, Font.PLAIN, 12));

            valueLabel = new JLabel(value);
            valueLabel.setFont(new Font(FONT_UI, Font.BOLD, 22));

            add(titleLabel, BorderLayout.NORTH);
            add(valueLabel, BorderLayout.CENTER);

            owner.statCards.add(this);
        }

        void applyTheme(boolean dark, Color panel, Color text, Color subtext, Color border) {
            setBackground(panel);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border, 1),
                    new EmptyBorder(14, 18, 14, 18)));
            titleLabel.setForeground(subtext);
            valueLabel.setForeground(text);
        }
    }

    // ===== HERO CARD =====
    private static class HeroCard extends JPanel {
        final JLabel valueLabel;
        final JLabel subLabel;

        HeroCard(String title, String value, String sub, Color accent, HypixelTimerApp owner) {
            putClientProperty("heroAccent", accent);
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(new EmptyBorder(18, 22, 18, 20));

            JPanel stack = new JPanel();
            stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
            stack.setOpaque(false);

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font(FONT_UI, Font.BOLD, 10));
            titleLabel.setForeground(new Color(190, 190, 200));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            valueLabel = new JLabel(value);
            valueLabel.setFont(new Font(FONT_UI, Font.BOLD, 26));
            valueLabel.setForeground(Color.WHITE);
            valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            valueLabel.setBorder(new EmptyBorder(4, 0, 2, 0));
            valueLabel.putClientProperty("customColor", Boolean.TRUE);

            subLabel = new JLabel(sub);
            subLabel.setFont(new Font(FONT_UI, Font.PLAIN, 12));
            subLabel.setForeground(new Color(200, 200, 215));
            subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            subLabel.putClientProperty("customColor", Boolean.TRUE);

            stack.add(titleLabel);
            stack.add(valueLabel);
            stack.add(subLabel);

            add(stack, BorderLayout.CENTER);

            owner.heroCards.add(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 16;

            Color accent = (Color) getClientProperty("heroAccent");
            if (accent == null) accent = new Color(60, 130, 220);

            float t = System.currentTimeMillis() / 1000f;
            float hueShift = (float) Math.sin(t * 0.35f) * 0.03f;
            Color animatedAccent = shiftHue(accent, hueShift);

            Color topBase = new Color(
                    Math.min(255, animatedAccent.getRed()   / 3 + 32),
                    Math.min(255, animatedAccent.getGreen() / 3 + 32),
                    Math.min(255, animatedAccent.getBlue()  / 3 + 42));

            Color accent2 = shiftHue(animatedAccent, 0.06f);
            Color topRight = new Color(
                    Math.min(255, accent2.getRed()   / 3 + 36),
                    Math.min(255, accent2.getGreen() / 3 + 36),
                    Math.min(255, accent2.getBlue()  / 3 + 46));

            float drift = (float) Math.sin(t * 0.5f) * 0.15f;
            g2.setPaint(new LinearGradientPaint(
                    new Point2D.Float(0, -h * drift),
                    new Point2D.Float(w, h * (1 + drift)),
                    new float[]{0f, 1f},
                    new Color[]{topBase, new Color(26, 26, 36)}));
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            RadialGradientPaint topBloom = new RadialGradientPaint(
                    new Point(w, 0), w * 0.9f,
                    new float[]{0f, 1f},
                    new Color[]{
                            new Color(topRight.getRed(), topRight.getGreen(), topRight.getBlue(), 110),
                            new Color(topRight.getRed(), topRight.getGreen(), topRight.getBlue(), 0)
                    });
            g2.setPaint(topBloom);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            g2.setColor(animatedAccent);
            g2.fillRoundRect(0, 0, 6, h, arc, arc);
            g2.fillRect(3, 0, 6, h);

            float glowAngle = t * 0.6f;
            int glowCx = (int) (w * 0.75 + Math.cos(glowAngle) * w * 0.10f);
            int glowCy = (int) (h * 0.35 + Math.sin(glowAngle * 1.3f) * h * 0.18f);
            float glowRadius = 100f + (float) Math.sin(t * 1.1f) * 20f;

            RadialGradientPaint glow = new RadialGradientPaint(
                    new Point(glowCx, glowCy), glowRadius,
                    new float[]{0f, 1f},
                    new Color[]{
                            new Color(animatedAccent.getRed(), animatedAccent.getGreen(), animatedAccent.getBlue(), 90),
                            new Color(animatedAccent.getRed(), animatedAccent.getGreen(), animatedAccent.getBlue(), 0)
                    });
            g2.setPaint(glow);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            int borderAlpha = (int) (110 + Math.sin(t * 1.4f) * 25);
            borderAlpha = Math.max(60, Math.min(180, borderAlpha));
            g2.setColor(new Color(animatedAccent.getRed(), animatedAccent.getGreen(), animatedAccent.getBlue(), borderAlpha));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

            g2.dispose();
        }
    }

    // ===== CALC RESULT CARD =====
    private static class CalcResultCard extends JPanel {
        final JLabel valueLabel;
        private final JLabel titleLabel;
        private final Color accent;

        CalcResultCard(String title, Color accent) {
            this.accent = accent;
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(new EmptyBorder(12, 16, 12, 16));

            JPanel stack = new JPanel();
            stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
            stack.setOpaque(false);

            titleLabel = new JLabel(title);
            titleLabel.setFont(new Font(FONT_UI, Font.BOLD, 10));
            titleLabel.setForeground(new Color(180, 180, 195));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            titleLabel.putClientProperty("customColor", Boolean.TRUE);

            valueLabel = new JLabel("\u2014");
            valueLabel.setFont(new Font(FONT_UI, Font.BOLD, 17));
            valueLabel.setForeground(accent);
            valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            valueLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
            valueLabel.putClientProperty("customColor", Boolean.TRUE);

            stack.add(titleLabel);
            stack.add(valueLabel);
            add(stack, BorderLayout.CENTER);
        }

        void setValue(String value, Color color) {
            valueLabel.setText(value);
            valueLabel.setForeground(color != null ? color : accent);
        }

        void setTitle(String title) {
            titleLabel.setText(title);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 12;

            g2.setPaint(new GradientPaint(0, 0, new Color(40, 42, 54), 0, h, new Color(28, 29, 38)));
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 210));
            g2.fillRoundRect(0, 0, w, 3, 3, 3);
            g2.fillRect(0, 1, w, 2);

            RadialGradientPaint glow = new RadialGradientPaint(
                    new Point(0, 0), w * 0.75f,
                    new float[]{0f, 1f},
                    new Color[]{
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40),
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0)
                    });
            g2.setPaint(glow);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            float t = System.currentTimeMillis() / 1000f;
            int borderAlpha = (int) (70 + Math.sin(t * 1.5f) * 20);
            borderAlpha = Math.max(40, Math.min(120, borderAlpha));
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), borderAlpha));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

            g2.dispose();
        }
    }

    // ===== MINECRAFT TITLE =====
    private static class MinecraftTitleLabel extends JLabel {
        private final boolean grassBlock;

        MinecraftTitleLabel(String text, int size, boolean grassBlock) {
            super(text, SwingConstants.CENTER);
            this.grassBlock = grassBlock;
            setFont(new Font(FONT_PIXEL, Font.BOLD, size));
            setOpaque(false);
            setBorder(new EmptyBorder(16, 24, 16, 24));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            if (grassBlock) { d.width += 24; d.height += 6; }
            return d;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

            String text = getText();
            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth(text);
            int ascent = fm.getAscent();
            int descent = fm.getDescent();
            int textH = ascent + descent;

            int textX = (getWidth() - textW) / 2;
            int textY = (getHeight() + ascent - descent) / 2;

            if (grassBlock) {
                int padX = 22, padY = 14;
                int blockX = textX - padX;
                int blockY = (getHeight() - textH) / 2 - padY;
                int blockW = textW + padX * 2;
                int blockH = textH + padY * 2;
                drawGrassBlock(g2, blockX, blockY, blockW, blockH);
            }

            g2.setColor(new Color(0, 0, 0, 210));
            g2.drawString(text, textX + 2, textY + 2);
            g2.drawString(text, textX + 3, textY + 2);
            g2.drawString(text, textX + 2, textY + 3);

            g2.setColor(Color.WHITE);
            g2.drawString(text, textX, textY);

            g2.dispose();
        }

        private void drawGrassBlock(Graphics2D g2, int x, int y, int w, int h) {
            int grassH = Math.max(8, h / 3);

            g2.setColor(new Color(134, 96, 67));
            g2.fillRect(x, y + grassH, w, h - grassH);

            g2.setColor(new Color(104, 72, 48));
            for (int dy = grassH; dy < h; dy += 4)
                for (int dx = 0; dx < w; dx += 4)
                    if (((dx * 13 + dy * 7) & 0xF) < 5) g2.fillRect(x + dx, y + dy, 3, 3);

            g2.setColor(new Color(158, 118, 82));
            for (int dy = grassH + 2; dy < h; dy += 6)
                for (int dx = 2; dx < w; dx += 6)
                    if (((dx * 5 + dy * 11) & 0xF) < 3) g2.fillRect(x + dx, y + dy, 2, 2);

            g2.setColor(new Color(88, 158, 62));
            g2.fillRect(x, y, w, grassH);

            g2.setColor(new Color(122, 200, 85));
            for (int dx = 0; dx < w; dx += 3) {
                int streak = (dx * 17) % 4;
                int colH = Math.max(2, grassH - 3 - streak);
                g2.fillRect(x + dx, y + 1, 2, colH);
            }

            g2.setColor(new Color(60, 118, 40));
            g2.fillRect(x, y + grassH - 3, w, 3);

            g2.setColor(new Color(134, 96, 67));
            for (int dx = 1; dx < w; dx += 5) {
                int bleed = (dx * 3) % 3;
                g2.fillRect(x + dx, y + grassH - 1 - bleed, 2, 2);
            }

            g2.setColor(new Color(20, 15, 10, 220));
            g2.drawRect(x, y, w - 1, h - 1);
        }
    }

    // ===== FLAT BUTTON =====
    private static class FlatButton extends JButton {
        enum Role { DEFAULT, ACCENT, SUCCESS, DANGER }

        private final Role role;
        private boolean dark;

        FlatButton(String text, Role role, HypixelTimerApp owner, ActionListener action) {
            super(text);
            this.role = role;
            this.dark = owner.darkMode;
            owner.flatButtons.add(this);

            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setFont(new Font(FONT_UI, Font.PLAIN, 14));
            setMargin(new Insets(12, 18, 12, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(action);
        }

        void setDark(boolean d) { this.dark = d; repaint(); }

        private Color baseColor() {
            switch (role) {
                case ACCENT:  return new Color(60, 130, 220);
                case SUCCESS: return new Color(60, 165, 95);
                case DANGER:  return new Color(200, 70, 70);
                default:      return dark ? new Color(52, 52, 62) : new Color(226, 228, 234);
            }
        }

        private Color textColor() {
            if (role == Role.DEFAULT) return dark ? new Color(240, 240, 245) : new Color(30, 30, 40);
            return Color.WHITE;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = baseColor();
            if (getModel().isRollover()) c = c.brighter();
            if (getModel().isPressed())  c = c.darker();
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();

            setForeground(textColor());
            super.paintComponent(g);
        }
    }

    // ===== XP ORB =====
    private static class ImageOrbLabel extends JPanel {
        private final String levelText;
        private final int level;
        private Timer pulseTimer;
        private float pulse = 0f;
        private boolean pulseForward = true;

        private static final int ORB_SIZE = 75;
        ImageOrbLabel(int level, String imageFilename, Color backgroundColor) {
            this.level = level;
            this.levelText = String.valueOf(level);

            setOpaque(true);
            setBackground(backgroundColor);
            setLayout(new BorderLayout());

            Dimension fixed = new Dimension(ORB_SIZE, ORB_SIZE);
            setPreferredSize(fixed);
            setMinimumSize(fixed);
            setMaximumSize(fixed);

            pulseTimer = new Timer(50, e -> {
                if (pulseForward) {
                    pulse += 0.03f;
                    if (pulse >= 1f) { pulse = 1f; pulseForward = false; }
                } else {
                    pulse -= 0.03f;
                    if (pulse <= 0f) { pulse = 0f; pulseForward = true; }
                }
                repaint();
            });
            pulseTimer.start();

            addComponentListener(new ComponentAdapter() {
                @Override public void componentHidden(ComponentEvent e) {
                    if (pulseTimer != null) pulseTimer.stop();
                }
                @Override public void componentShown(ComponentEvent e) {
                    if (pulseTimer != null && !pulseTimer.isRunning()) pulseTimer.start();
                }
            });
        }

        @Override public Dimension getPreferredSize() { return new Dimension(ORB_SIZE, ORB_SIZE); }
        @Override public Dimension getMinimumSize()   { return new Dimension(ORB_SIZE, ORB_SIZE); }
        @Override public Dimension getMaximumSize()   { return new Dimension(ORB_SIZE, ORB_SIZE); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2;
            int radius = Math.min(cx, cy) - 10;
            if (radius < 15) radius = 15;

            drawOrb(g2, cx, cy, radius);
            drawLevelText(g2, cx, cy);

            g2.dispose();
        }

        private void drawOrb(Graphics2D g2, int cx, int cy, int radius) {
            for (int i = 8; i >= 1; i--) {
                int alpha = (int) ((6 + pulse * 10) * (9 - i) / 8);
                g2.setColor(new Color(120, 255, 60, Math.min(alpha, 60)));
                g2.fillOval(cx - radius - i, cy - radius - i,
                        (radius + i) * 2, (radius + i) * 2);
            }

            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillOval(cx - radius + 2, cy - radius + 4, radius * 2, radius * 2);

            float hlX = cx - radius * 0.35f;
            float hlY = cy - radius * 0.35f;
            RadialGradientPaint rgp = new RadialGradientPaint(
                    new Point((int) hlX, (int) hlY), radius * 1.6f,
                    new float[]{0.0f, 0.45f, 0.85f, 1.0f},
                    new Color[]{
                            new Color(220, 255, 150),
                            new Color(140, 235, 60),
                            new Color(60, 180, 30),
                            new Color(15, 80, 10)
                    });
            g2.setPaint(rgp);
            g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

            g2.setColor(new Color(255, 255, 255, 150));
            int specW = (int) (radius * 0.9);
            int specH = (int) (radius * 0.55);
            g2.fillOval(cx - radius + 5, cy - radius + 5, specW, specH);

            g2.setColor(new Color(0, 50, 0, 100));
            g2.setStroke(new BasicStroke(2f));
            g2.drawArc(cx - radius + 3, cy - radius + 3,
                    radius * 2 - 6, radius * 2 - 6, 200, 140);

            g2.setColor(new Color(10, 60, 5, 230));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }

        private void drawLevelText(Graphics2D g2, int cx, int cy) {
            int fontSize = levelText.length() >= 3 ? 20 : 26;
            g2.setFont(new Font(FONT_PIXEL, Font.BOLD, fontSize));

            FontMetrics fm = g2.getFontMetrics();
            int tx = cx - fm.stringWidth(levelText) / 2;
            int ty = cy + (fm.getAscent() - fm.getDescent()) / 2;

            g2.setColor(new Color(0, 0, 0, 220));
            for (int dx = -1; dx <= 2; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    g2.drawString(levelText, tx + dx, ty + dy);
                }
            }

            g2.setColor(new Color(30, 100, 20));
            g2.drawString(levelText, tx + 1, ty + 1);

            g2.setColor(Color.WHITE);
            g2.drawString(levelText, tx, ty);
        }
    }

    // ===== INFO CARD =====
    private static class InfoCard extends JPanel {
        InfoCard(String title, String value, String subText) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(true);
            putClientProperty("card", Boolean.TRUE);

            JLabel titleLbl = new JLabel(title);
            titleLbl.setFont(new Font(FONT_UI, Font.BOLD, 11));
            titleLbl.putClientProperty("subtext", Boolean.TRUE);
            titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel valueLbl = new JLabel(value);
            valueLbl.setFont(new Font(FONT_UI, Font.BOLD, 18));
            valueLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            valueLbl.setBorder(new EmptyBorder(4, 0, 2, 0));

            add(titleLbl);
            add(valueLbl);

            if (subText != null && !subText.isEmpty()) {
                JLabel subLbl = new JLabel(subText);
                subLbl.setFont(new Font(FONT_UI, Font.PLAIN, 11));
                subLbl.putClientProperty("subtext", Boolean.TRUE);
                subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                add(subLbl);
            }
        }
    }

    // ===== JAVA VERSION GUARD =====
    private static final int REQUIRED_JAVA_VERSION = 25;

    private static boolean checkJavaVersion() {
        int current;
        try {
            current = Runtime.version().feature();
        } catch (Throwable t) {
            try {
                String v = System.getProperty("java.version", "0");
                int dot = v.indexOf('.');
                current = Integer.parseInt(dot > 0 ? v.substring(0, dot) : v);
            } catch (Throwable ignored) {
                current = 0;
            }
        }

        if (current >= REQUIRED_JAVA_VERSION) return true;

        String msg = "<html><body style='width:400px;font-family:Segoe UI'>"
                + "<b>Unsupported Java version</b><br><br>"
                + "This application requires <b>Java " + REQUIRED_JAVA_VERSION + " or newer</b>.<br>"
                + "You are currently running <b>Java " + current + "</b>.<br><br>"
                + "Please install the latest <b>Eclipse Temurin (Adoptium) JDK "
                + REQUIRED_JAVA_VERSION + "</b> and launch the app with that version.<br><br>"
                + "Click <b>OK</b> to open the download page in your browser.";
        try {
            JOptionPane.showMessageDialog(null, msg,
                    "Java Version Error", JOptionPane.ERROR_MESSAGE);
        } catch (Throwable ignored) {}

        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(
                        "https://adoptium.net/temurin/releases/?version=" + REQUIRED_JAVA_VERSION
                                + "&os=any&arch=any&package=jdk"));
            }
        } catch (Throwable ignored) {}

        return false;
    }

    public static void main(String[] args) {
        if (!checkJavaVersion()) {
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> new HypixelTimerApp().setVisible(true));
    }
}