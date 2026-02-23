import 'package:fl_chart/fl_chart.dart';
import 'package:table_calendar/table_calendar.dart';
import 'transaction_detail_screen.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../models/transaction_model.dart';
import '../models/category_model.dart';
import '../providers/transaction_provider.dart';
import '../providers/category_provider.dart';
import '../l10n/app_localizations.dart';
import '../utils/constants.dart';
import '../services/api_service.dart';
import '../providers/auth_provider.dart';

class StatsScreen extends StatefulWidget {
  const StatsScreen({super.key});

  @override
  State<StatsScreen> createState() => _StatsScreenState();
}

class _StatsScreenState extends State<StatsScreen> {
  String _period = "month";
  DateTime _displayDate = DateTime.now();
  final Map<int, String> _topCategoryForBar = {};

  final ApiService _apiService = ApiService();

  // Structured insights
  List<Map<String, dynamic>> _insights = [];
  bool _isLoadingInsights = false;

  String? _periodAiInsight;
  bool _isLoadingPeriodInsight = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _fetchInsights();
      _fetchPeriodAiInsight();
    });
  }

  Future<void> _fetchInsights() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    if (authProvider.currentUserId == null || authProvider.token == null) return;

    setState(() => _isLoadingInsights = true);
    try {
      final result = await _apiService.getInsights(
          authProvider.currentUserId!, authProvider.token!);
      if (mounted) {
        setState(() {
          _insights = result.cast<Map<String, dynamic>>();
        });
      }
    } catch (e) {
      debugPrint('Failed to load insights: $e');
    } finally {
      if (mounted) setState(() => _isLoadingInsights = false);
    }
  }

  Future<void> _fetchPeriodAiInsight() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    if (authProvider.currentUserId == null || authProvider.token == null) return;

    setState(() => _isLoadingPeriodInsight = true);
    try {
      final dateStr = DateFormat('yyyy-MM-dd').format(_displayDate);
      final insight = await _apiService.getPeriodInsight(
          authProvider.currentUserId!, _period, dateStr, authProvider.token!);
      if (mounted) {
        setState(() {
          _periodAiInsight = insight;
        });
      }
    } catch (e) {
      debugPrint('Failed to load period insight: $e');
    } finally {
      if (mounted) setState(() => _isLoadingPeriodInsight = false);
    }
  }

  Future<void> _showCategoryInsight(String category) async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    if (authProvider.currentUserId == null || authProvider.token == null) return;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => _CategoryInsightSheet(
        category: category,
        userId: authProvider.currentUserId!,
        token: authProvider.token!,
        apiService: _apiService,
      ),
    );
  }

  void _moveDate(int delta) {
    setState(() {
      if (_period == "day") {
        _displayDate = _displayDate.add(Duration(days: delta));
      } else if (_period == "month") {
        _displayDate = DateTime(_displayDate.year, _displayDate.month + delta);
      } else if (_period == "year") {
        _displayDate = DateTime(_displayDate.year + delta);
      } else {
        _displayDate = _displayDate.add(Duration(days: 7 * delta));
      }
    });
    _fetchPeriodAiInsight();
  }

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<TransactionProvider>(context);
    final catProvider = Provider.of<CategoryProvider>(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    final filteredTx = provider.transactions.where((t) {
      if (_period == "day") {
        return t.date.day == _displayDate.day &&
            t.date.month == _displayDate.month &&
            t.date.year == _displayDate.year;
      } else if (_period == "week") {
        final startOfWeek =
            _displayDate.subtract(Duration(days: _displayDate.weekday - 1));
        final endOfWeek = startOfWeek.add(const Duration(days: 6));
        return t.date.isAfter(
                DateTime(startOfWeek.year, startOfWeek.month, startOfWeek.day)
                    .subtract(const Duration(seconds: 1))) &&
            t.date.isBefore(
                DateTime(endOfWeek.year, endOfWeek.month, endOfWeek.day)
                    .add(const Duration(days: 1)));
      } else if (_period == "month") {
        return t.date.month == _displayDate.month &&
            t.date.year == _displayDate.year;
      } else {
        return t.date.year == _displayDate.year;
      }
    }).toList();

    double income = filteredTx
        .where((t) => t.type == 'INCOME')
        .fold(0, (sum, t) => sum + t.amount);
    double expense = filteredTx
        .where((t) => t.type == 'EXPENSE')
        .fold(0, (sum, t) => sum + t.amount);

    List<BarChartGroupData> barGroups = [];
    double maxY = 0;
    _topCategoryForBar.clear();

    if (_period == "week") {
      for (int i = 1; i <= 7; i++) {
        final dayTx = filteredTx
            .where((t) => t.type == 'EXPENSE' && t.date.weekday == i)
            .toList();
        _trackTopCategory(i, dayTx);
        final group = _makeStackedBarData(i, dayTx, catProvider, isDark);
        double sum = dayTx.fold(0, (s, t) => s + t.amount);
        if (sum > maxY) maxY = sum;
        barGroups.add(group);
      }
    } else if (_period == "month") {
      int daysInMonth =
          DateTime(_displayDate.year, _displayDate.month + 1, 0).day;
      for (int i = 1; i <= daysInMonth; i++) {
        final dayTx = filteredTx
            .where((t) => t.type == 'EXPENSE' && t.date.day == i)
            .toList();
        _trackTopCategory(i, dayTx);
        final group = _makeStackedBarData(i, dayTx, catProvider, isDark);
        double sum = dayTx.fold(0, (s, t) => s + t.amount);
        if (sum > maxY) maxY = sum;
        barGroups.add(group);
      }
    } else if (_period == "year") {
      for (int i = 1; i <= 12; i++) {
        final monthTx = filteredTx
            .where((t) => t.type == 'EXPENSE' && t.date.month == i)
            .toList();
        _trackTopCategory(i, monthTx);
        final group = _makeStackedBarData(i, monthTx, catProvider, isDark);
        double sum = monthTx.fold(0, (s, t) => s + t.amount);
        if (sum > maxY) maxY = sum;
        barGroups.add(group);
      }
    }

    Map<String, double> catExpenses = {};
    for (var tx in filteredTx) {
      if (tx.type == 'EXPENSE') {
        catExpenses[tx.category] = (catExpenses[tx.category] ?? 0) + tx.amount;
      }
    }

    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
          title: Text(l10n.translate('analytics')),
          backgroundColor: AppColors.primary,
          foregroundColor: Colors.white,
          elevation: 0),
      body: RefreshIndicator(
        onRefresh: () => provider.fetchTransactions(),
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: Column(
            children: [
              _buildPeriodSelector(),
              _buildDateNavigator(isDark),
              Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    Expanded(
                        child: _buildSummaryCard(
                            l10n.translate('income'), income, Colors.green, isDark)),
                    const SizedBox(width: 16),
                    Expanded(
                        child: _buildSummaryCard(
                            l10n.translate('expense'), expense, Colors.redAccent, isDark)),
                  ],
                ),
              ),

              _buildAiInsightSection(isDark),

              if (_period != "day")
                Container(
                  margin: const EdgeInsets.symmetric(horizontal: 16),
                  padding: const EdgeInsets.fromLTRB(10, 24, 20, 12),
                  decoration: BoxDecoration(
                      color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
                      borderRadius: BorderRadius.circular(24),
                      boxShadow: [
                        BoxShadow(
                            color: Colors.black.withValues(alpha: 0.02), blurRadius: 10)
                      ]),
                  child: _period == "month"
                      ? _buildCalendar(filteredTx, isDark)
                      : Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Padding(
                              padding: const EdgeInsets.only(left: 12.0),
                              child: Text(l10n.translate('spending_dynamics'),
                                  style: const TextStyle(
                                      fontSize: 17,
                                      fontWeight: FontWeight.bold)),
                            ),
                            const SizedBox(height: 30),
                            SizedBox(
                              height: 250,
                              child: BarChart(
                                BarChartData(
                                  maxY: maxY == 0 ? 100 : maxY * 1.3,
                                  gridData: FlGridData(
                                    show: true,
                                    drawVerticalLine: false,
                                    horizontalInterval:
                                        maxY > 0 ? maxY / 4 : 25,
                                    getDrawingHorizontalLine: (value) =>
                                        FlLine(
                                      color: isDark
                                          ? Colors.white10
                                          : Colors.grey[200]!,
                                      strokeWidth: 1,
                                    ),
                                  ),
                                  barTouchData: BarTouchData(
                                    touchCallback: (event, response) {
                                      if (event is FlTapUpEvent &&
                                          response != null &&
                                          response.spot != null) {
                                        final index = response
                                            .spot!.touchedBarGroupIndex;
                                        if (index >= 0 &&
                                            index < barGroups.length) {
                                          final x = barGroups[index].x;
                                          _onBarTapped(x);
                                        }
                                      }
                                    },
                                    touchTooltipData: BarTouchTooltipData(
                                      tooltipBgColor: isDark
                                          ? const Color(0xFF333333)
                                          : const Color(0xFF2E3440),
                                      getTooltipItem:
                                          (group, groupIndex, rod, rodIndex) {
                                        return BarTooltipItem(
                                          "\$${rod.toY.toStringAsFixed(0)}",
                                          const TextStyle(
                                              color: Colors.white,
                                              fontWeight: FontWeight.bold),
                                        );
                                      },
                                    ),
                                  ),
                                  borderData: FlBorderData(show: false),
                                  titlesData: FlTitlesData(
                                    topTitles: AxisTitles(
                                      sideTitles: SideTitles(
                                        showTitles: true,
                                        getTitlesWidget: (val, meta) =>
                                            _getTopTitles(
                                                val, barGroups, catProvider),
                                        reservedSize: 30,
                                      ),
                                    ),
                                    rightTitles: const AxisTitles(
                                        sideTitles:
                                            SideTitles(showTitles: false)),
                                    leftTitles: AxisTitles(
                                      sideTitles: SideTitles(
                                        showTitles: true,
                                        reservedSize: 45,
                                        getTitlesWidget: (val, meta) => Text(
                                            "\$${val.toInt()}",
                                            style: const TextStyle(
                                                color: Colors.grey,
                                                fontSize: 10)),
                                      ),
                                    ),
                                    bottomTitles: AxisTitles(
                                        sideTitles: SideTitles(
                                            showTitles: true,
                                            getTitlesWidget: (val, meta) =>
                                                _getBottomTitles(
                                                    val, _period),
                                            reservedSize: 35)),
                                  ),
                                  barGroups: barGroups,
                                ),
                              ),
                            ),
                          ],
                        ),
                ),

              if (_period == "day")
                _buildDayTransactionsList(filteredTx, isDark),

              _buildCategorySection(catExpenses, expense, isDark),
              const SizedBox(height: 30),
            ],
          ),
        ),
      ),
    );
  }

  void _onBarTapped(int x) {
    setState(() {
      if (_period == "year") {
        _period = "month";
        _displayDate = DateTime(_displayDate.year, x);
      } else if (_period == "month") {
        _period = "week";
        _displayDate = DateTime(_displayDate.year, _displayDate.month, x);
      } else if (_period == "week") {
        _period = "day";
        final startOfWeek = _displayDate.subtract(Duration(days: _displayDate.weekday - 1));
        _displayDate = startOfWeek.add(Duration(days: x - 1));
      }
    });
  }

  void _trackTopCategory(int x, List<TransactionModel> txs) {
    if (txs.isEmpty) return;
    Map<String, double> sums = {};
    for (var t in txs) {
      sums[t.category] = (sums[t.category] ?? 0) + t.amount;
    }
    String topCat = sums.entries.reduce((a, b) => a.value > b.value ? a : b).key;
    _topCategoryForBar[x] = topCat;
  }

  Widget _getTopTitles(double value, List<BarChartGroupData> groups, CategoryProvider catProvider) {
    final x = value.toInt();
    if (!_topCategoryForBar.containsKey(x)) return const SizedBox();
    
    final catName = _topCategoryForBar[x]!;
    final catInfo = catProvider.categories.firstWhere((c) => c.name == catName, orElse: () => CategoryModel(name: catName, icon: 'category', color: '#9E9E9E', type: 'EXPENSE'));
    
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Icon(
        CategoryUtils.getIconData(catInfo.icon),
        size: 16,
        color: AppColors.primary.withValues(alpha: 0.8),
      ),
    );
  }

  Widget _buildPeriodSelector() {
    final l10n = AppLocalizations.of(context)!;
    final periods = [
      {'key': 'day', 'label': l10n.translate('day')},
      {'key': 'week', 'label': l10n.translate('week')},
      {'key': 'month', 'label': l10n.translate('month')},
      {'key': 'year', 'label': l10n.translate('year')},
    ];

    return Container(
      color: AppColors.primary,
      padding: const EdgeInsets.only(bottom: 16, left: 16, right: 16),
      child: Container(
        height: 48,
        decoration: BoxDecoration(
          color: Colors.black.withValues(alpha: 0.15),
          borderRadius: BorderRadius.circular(24),
        ),
        child: Row(
          children: periods.map((e) {
            final isSel = _period == e['key'];
            return Expanded(
              child: GestureDetector(
                onTap: () {
                  if (_period != e['key']!) {
                    setState(() {
                      _period = e['key']!;
                      _displayDate = DateTime.now();
                    });
                    _fetchPeriodAiInsight();
                  }
                },
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 250),
                  margin: const EdgeInsets.all(4),
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: isSel ? Colors.white : Colors.transparent,
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: isSel ? [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.1),
                        blurRadius: 4,
                        offset: const Offset(0, 2),
                      )
                    ] : [],
                  ),
                  child: Text(
                    e['label']!,
                    style: TextStyle(
                      color: isSel ? AppColors.primary : Colors.white.withValues(alpha: 0.9),
                      fontWeight: isSel ? FontWeight.bold : FontWeight.w500,
                      fontSize: 14,
                    ),
                  ),
                ),
              ),
            );
          }).toList(),
        ),
      ),
    );
  }

  Widget _buildDateNavigator(bool isDark) {
    final l10n = AppLocalizations.of(context)!;
    String label = "";
    if (_period == "day") {
      label = DateFormat('dd MMMM yyyy', l10n.locale.languageCode).format(_displayDate);
    } else if (_period == "month") {
      label = DateFormat('MMMM yyyy', l10n.locale.languageCode).format(_displayDate);
    } else if (_period == "year") {
      label = "${_displayDate.year} ${l10n.translate('year')}";
    } else {
      final startOfWeek =
          _displayDate.subtract(Duration(days: _displayDate.weekday - 1));
      final endOfWeek = startOfWeek.add(const Duration(days: 6));
      label =
          "${startOfWeek.day}.${startOfWeek.month} - ${endOfWeek.day}.${endOfWeek.month}";
    }

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
              onPressed: () => _moveDate(-1),
              icon: const Icon(Icons.chevron_left)),
          Text(label,
              style:
                  const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          IconButton(
              onPressed: () => _moveDate(1),
              icon: const Icon(Icons.chevron_right)),
        ],
      ),
    );
  }

  BarChartGroupData _makeStackedBarData(int x, List<TransactionModel> txs, CategoryProvider catProvider, bool isDark) {
    Map<String, double> catSums = {};
    for (var tx in txs) {
      catSums[tx.category] = (catSums[tx.category] ?? 0) + tx.amount;
    }

    double currentY = 0;
    List<BarChartRodStackItem> stackItems = [];
    
    // Sort to have consistent stacking
    final sortedCats = catSums.keys.toList()..sort();
    
    for (var cat in sortedCats) {
      final amount = catSums[cat]!;
      final catInfo = catProvider.categories.firstWhere((c) => c.name == cat, orElse: () => CategoryModel(name: cat, icon: 'category', color: '#9E9E9E', type: 'EXPENSE'));
      final color = Color(int.parse(catInfo.color.replaceAll('#', '0xFF')));
      
      stackItems.add(BarChartRodStackItem(currentY, currentY + amount, color));
      currentY += amount;
    }

    return BarChartGroupData(
      x: x, 
      barRods: [
        BarChartRodData(
          toY: currentY,
          width: _period == "month" ? 6 : 18,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(4)),
          rodStackItems: stackItems,
          backDrawRodData: BackgroundBarChartRodData(
              show: true,
              toY: 0,
              color: isDark ? Colors.white10 : Colors.grey[50]!)
        ),
      ]
    );
  }

  Widget _getBottomTitles(double value, String period) {
    final l10n = AppLocalizations.of(context)!;
    int val = value.toInt();
    String text = "";
    if (period == "week") {
      final days = [
        l10n.translate('mon'),
        l10n.translate('tue'),
        l10n.translate('wed'),
        l10n.translate('thu'),
        l10n.translate('fri'),
        l10n.translate('sat'),
        l10n.translate('sun'),
      ];
      if (val >= 1 && val <= 7) {
        final startOfWeek =
            _displayDate.subtract(Duration(days: _displayDate.weekday - 1));
        final currentDay = startOfWeek.add(Duration(days: val - 1));
        text = "${days[val - 1]}\n${currentDay.day}";
      }
    } else if (period == "month") {
      if (val % 7 == 0 || val == 1) text = val.toString();
    } else {
      final months = [
        l10n.translate('jan'),
        l10n.translate('feb'),
        l10n.translate('mar'),
        l10n.translate('apr'),
        l10n.translate('may'),
        l10n.translate('jun'),
        l10n.translate('jul'),
        l10n.translate('aug'),
        l10n.translate('sep'),
        l10n.translate('oct'),
        l10n.translate('nov'),
        l10n.translate('dec'),
      ];
      if (val >= 1 && val <= 12) text = months[val - 1];
    }
    return Padding(
      padding: const EdgeInsets.only(top: 6.0),
      child: Text(text,
          textAlign: TextAlign.center,
          style:
              const TextStyle(color: Colors.grey, fontSize: 10, height: 1.2)),
    );
  }

  Widget _buildSummaryCard(
      String title, double amount, Color color, bool isDark) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
          color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: color.withValues(alpha: 0.1))),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(color: Colors.grey, fontSize: 13)),
          const SizedBox(height: 8),
          FittedBox(
              child: Text("\$${amount.toStringAsFixed(0)}",
                  style: TextStyle(
                      color: color,
                      fontSize: 22,
                      fontWeight: FontWeight.bold))),
        ],
      ),
    );
  }

  Widget _buildDayTransactionsList(List<TransactionModel> txs, bool isDark) {
    final expenses = txs.where((t) => t.type == 'EXPENSE').toList();
    final l10n = AppLocalizations.of(context)!;

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
          color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
          borderRadius: BorderRadius.circular(24)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(l10n.translate('transactions'), style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          if (expenses.isEmpty)
             Center(child: Padding(
               padding: const EdgeInsets.all(20.0),
               child: Text(l10n.translate('no_transactions')),
             ))
          else
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: expenses.length,
              separatorBuilder: (_, __) => const Divider(height: 24),
              itemBuilder: (context, index) {
                final tx = expenses[index];
                return InkWell(
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => TransactionDetailScreen(transaction: tx),
                      ),
                    ).then((_) {
                      Provider.of<TransactionProvider>(context, listen: false).fetchTransactions();
                    });
                  },
                  borderRadius: BorderRadius.circular(12),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8.0, horizontal: 4.0),
                    child: Row(
                      children: [
                        CircleAvatar(
                          backgroundColor: Colors.red.withValues(alpha: 0.1),
                          child: const Icon(Icons.south_west, color: Colors.red, size: 18),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(tx.category, style: const TextStyle(fontWeight: FontWeight.bold)),
                              if (tx.description.isNotEmpty)
                                Text(tx.description, style: const TextStyle(color: Colors.grey, fontSize: 12)),
                            ],
                          ),
                        ),
                         Text("-\$${tx.amount.toStringAsFixed(0)}", style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.red)),
                      ],
                    ),
                  ),
                );
              },
            ),
        ],
      ),
    );
  }

  Widget _buildCategorySection(
      Map<String, double> catExpenses, double total, bool isDark) {
    final l10n = AppLocalizations.of(context)!;
    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
          color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
          borderRadius: BorderRadius.circular(24)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(l10n.translate('top_spending'),
              style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
          const SizedBox(height: 20),
          if (catExpenses.isEmpty)
            Center(child: Text(l10n.translate('no_data')))
          else
            ...catExpenses.entries.map((e) {
              double pct = total == 0 ? 0 : (e.value / total);
              return InkWell(
                onTap: () => _showCategoryInsight(e.key),
                borderRadius: BorderRadius.circular(12),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 4),
                  child: Column(
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(e.key,
                              style:
                                  const TextStyle(fontWeight: FontWeight.w500)),
                          Text("\$${e.value.toStringAsFixed(0)}",
                              style:
                                  const TextStyle(fontWeight: FontWeight.bold)),
                        ],
                      ),
                      const SizedBox(height: 8),
                      LinearProgressIndicator(
                          value: pct,
                          color: AppColors.primary,
                          backgroundColor:
                              isDark ? Colors.white10 : Colors.grey[100],
                          minHeight: 8,
                          borderRadius: BorderRadius.circular(10)),
                    ],
                  ),
                ),
              );
            }).toList(),
        ],
      ),
    );
  }

  
  Widget _buildCalendar(List<TransactionModel> txs, bool isDark) {
    Map<DateTime, List<TransactionModel>> events = {};
    for (var tx in txs) {
      final date = DateTime(tx.date.year, tx.date.month, tx.date.day);
      if (events[date] == null) events[date] = [];
      events[date]!.add(tx);
    }

    return TableCalendar(
      firstDay: DateTime.utc(2020, 10, 16),
      lastDay: DateTime.utc(2030, 3, 14),
      focusedDay: _displayDate,
      calendarFormat: CalendarFormat.month,
      headerVisible: false,
      startingDayOfWeek: StartingDayOfWeek.monday,
      daysOfWeekStyle: DaysOfWeekStyle(
        weekdayStyle: TextStyle(color: isDark ? Colors.white70 : Colors.black87),
        weekendStyle: TextStyle(color: isDark ? Colors.redAccent : Colors.red),
      ),
      calendarStyle: CalendarStyle(
        defaultTextStyle: TextStyle(color: isDark ? Colors.white : Colors.black87),
        weekendTextStyle: TextStyle(color: isDark ? Colors.redAccent : Colors.red),
        outsideTextStyle: TextStyle(color: isDark ? Colors.white24 : Colors.grey),
      ),
      selectedDayPredicate: (day) {
        return isSameDay(_displayDate, day);
      },
      onDaySelected: (selectedDay, focusedDay) {
        setState(() {
          _displayDate = selectedDay;
          _period = "day"; // Switch to day view
        });
        _fetchPeriodAiInsight();
      },
      calendarBuilders: CalendarBuilders(
        markerBuilder: (context, date, events) {
          if (events.isNotEmpty) {
            return Positioned(
              right: 1,
              bottom: 1,
              child: Container(
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: AppColors.primary,
                ),
                width: 6.0,
                height: 6.0,
              ),
            ); 
          }
          return null;
        },
        defaultBuilder: (context, date, focusedDay) {
           if (events[DateTime(date.year, date.month, date.day)] != null) {
             return Container(
               margin: const EdgeInsets.all(4.0),
               alignment: Alignment.center,
               decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: 0.1),
                 shape: BoxShape.circle,
               ),
               child: Text(
                 date.day.toString(),
                 style: TextStyle(color: isDark ? Colors.white : Colors.black87),
               ),
             );
           }
           return null;
        }
      ),
      eventLoader: (day) {
        return events[DateTime(day.year, day.month, day.day)] ?? [];
      },
    );
  }

  Widget _buildAiInsightSection(bool isDark) {
    final l10n = AppLocalizations.of(context)!;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                l10n.translate('ai_insights'),
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              IconButton(
                onPressed: _isLoadingInsights ? null : () => _fetchInsights(),
                icon: Icon(Icons.refresh, size: 20, color: AppColors.primary),
                tooltip: "Reload global insights",
              ),
            ],
          ),
          
          // Period Insight Button/Card
          _buildPeriodInsightCard(isDark),
          const SizedBox(height: 16),

          // Global Insights
          if (_isLoadingInsights)
            const Center(
              child: Padding(
                padding: EdgeInsets.symmetric(vertical: 20),
                child: CircularProgressIndicator(),
              ),
            )
          else if (_insights.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 16),
              child: Center(
                child: Text(
                  l10n.translate('no_insights'),
                  style: TextStyle(
                    color: isDark ? Colors.white54 : Colors.black45,
                    fontSize: 14,
                  ),
                ),
              ),
            )
          else
            ..._insights.map((insight) => _buildInsightCard(insight, isDark)),
        ],
      ),
    );
  }

  Widget _buildPeriodInsightCard(bool isDark) {
    final l10n = AppLocalizations.of(context)!;
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: isDark
              ? [const Color(0xFF2D1B69), const Color(0xFF1A1A2E)]
              : [const Color(0xFFEDE7F6), const Color(0xFFF3E5F5)],
        ),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: isDark ? Colors.white10 : Colors.deepPurple.withOpacity(0.1),
        ),
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  const Icon(Icons.auto_awesome, color: Colors.deepPurple, size: 18),
                  const SizedBox(width: 8),
                  Text(
                    "${_period.toUpperCase()} ${l10n.translate('ai_summary').toUpperCase()}",
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: Colors.deepPurple,
                    ),
                  ),
                ],
              ),
              if (_isLoadingPeriodInsight)
                const SizedBox(
                  width: 14, height: 14,
                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.deepPurple),
                )
              else
                InkWell(
                  onTap: _fetchPeriodAiInsight,
                  child: const Icon(Icons.refresh, size: 16, color: Colors.deepPurple),
                ),
            ],
          ),
          const SizedBox(height: 10),
          if (_periodAiInsight != null)
            Text(
              _periodAiInsight!,
              style: TextStyle(
                fontSize: 13,
                color: isDark ? Colors.white70 : Colors.black87,
                height: 1.4,
              ),
            )
          else if (!_isLoadingPeriodInsight)
            Text(
              l10n.translate('no_summary_available'),
              style: TextStyle(fontSize: 13, color: isDark ? Colors.white54 : Colors.grey),
            ),
        ],
      ),
    );
  }

  Widget _buildInsightCard(Map<String, dynamic> insight, bool isDark) {
    final type = insight['type'] ?? 'INFO';
    final title = insight['title'] ?? '';
    final description = insight['description'] ?? '';
    final suggestedAction = insight['suggestedAction'] ?? '';
    final pctChange = (insight['percentageChange'] ?? 0).toDouble();
    final l10n = AppLocalizations.of(context)!;

    IconData icon;
    Color color;
    switch (type) {
      case 'WARNING':
        icon = Icons.trending_up;
        color = Colors.orange;
        break;
      case 'ALERT':
        icon = Icons.warning_amber_rounded;
        color = Colors.red;
        break;
      case 'TIP':
        icon = Icons.trending_down;
        color = Colors.green;
        break;
      default:
        icon = Icons.info_outline;
        color = Colors.blue;
    }

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: isDark 
          ? color.withValues(alpha: 0.1) 
          : color.withValues(alpha: 0.05),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withValues(alpha: 0.2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: color, size: 22),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  title,
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                    color: isDark ? Colors.white : Colors.black87,
                  ),
                ),
              ),
              if (pctChange != 0)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: pctChange > 0 
                      ? Colors.red.withValues(alpha: 0.15) 
                      : Colors.green.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    '${pctChange > 0 ? '+' : ''}${pctChange.toStringAsFixed(0)}%',
                    style: TextStyle(
                      color: pctChange > 0 ? Colors.red : Colors.green,
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            description,
            style: TextStyle(
              fontSize: 13,
              color: isDark ? Colors.white70 : Colors.black54,
              height: 1.4,
            ),
          ),
          if (suggestedAction.isNotEmpty) ...[
            const SizedBox(height: 8),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.lightbulb_outline, size: 14, color: color),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    '${l10n.translate('suggested_action')}: $suggestedAction',
                    style: TextStyle(
                      fontSize: 12,
                      fontStyle: FontStyle.italic,
                      color: isDark ? Colors.white60 : Colors.black45,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _CategoryInsightSheet extends StatefulWidget {
  final String category;
  final int userId;
  final String token;
  final ApiService apiService;

  const _CategoryInsightSheet({
    required this.category,
    required this.userId,
    required this.token,
    required this.apiService,
  });

  @override
  State<_CategoryInsightSheet> createState() => _CategoryInsightSheetState();
}

class _CategoryInsightSheetState extends State<_CategoryInsightSheet> {
  String? _insight;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchInsight();
  }

  Future<void> _fetchInsight() async {
    try {
      final res = await widget.apiService.getCategoryInsight(
          widget.userId, widget.category, widget.token);
      if (mounted) {
        setState(() {
          _insight = res;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final l10n = AppLocalizations.of(context)!;

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(32)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                widget.category,
                style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
              ),
              IconButton(
                onPressed: () => Navigator.pop(context),
                icon: const Icon(Icons.close),
              )
            ],
          ),
          const SizedBox(height: 20),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: isDark
                    ? [const Color(0xFF2D1B69), const Color(0xFF1A1A2E)]
                    : [const Color(0xFFEDE7F6), const Color(0xFFF3E5F5)],
              ),
              borderRadius: BorderRadius.circular(24),
            ),
            child: _isLoading
                ? const Center(
                    child: CircularProgressIndicator(color: Colors.deepPurple))
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          const Icon(Icons.auto_awesome, color: Colors.deepPurple, size: 20),
                          const SizedBox(width: 8),
                          Text(
                            l10n.translate('category_analysis').toUpperCase(),
                            style: const TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                              color: Colors.deepPurple,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      Text(
                        _insight ?? l10n.translate('no_summary_available'),
                        style: TextStyle(
                          fontSize: 15,
                          color: isDark ? Colors.white70 : Colors.black87,
                          height: 1.5,
                        ),
                      ),
                    ],
                  ),
          ),
          const SizedBox(height: 40),
        ],
      ),
    );
  }
}

