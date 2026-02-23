import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/transaction_provider.dart';
import '../providers/auth_provider.dart';
import '../providers/category_provider.dart';
import '../l10n/app_localizations.dart';
import '../utils/constants.dart';
import 'add_transaction_screen.dart';
import 'all_transactions_screen.dart';
import 'transaction_detail_screen.dart';
import 'stats_screen.dart';
import 'profile_screen.dart';
import 'notification_screen.dart';
import 'categories_screen.dart';
import 'budget_screen.dart';
import 'options_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _selectedIndex = 0;

  @override
  void initState() {
    super.initState();
    // Fetch data after the first frame or immediately if providers are ready
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        Provider.of<TransactionProvider>(context, listen: false)
            .fetchTransactions();
        Provider.of<CategoryProvider>(context, listen: false).fetchCategories();
      }
    });
  }

  final List<Widget> _pages = [
    const HomeContent(),
    const StatsScreen(),
    const ProfileScreen(),
    const OptionsScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: IndexedStack(
        index: _selectedIndex,
        children: _pages,
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _selectedIndex,
        selectedItemColor: AppColors.primary,
        unselectedItemColor: Colors.grey,
        showUnselectedLabels: true,
        type: BottomNavigationBarType.fixed,
        onTap: (index) => setState(() => _selectedIndex = index),
        items: [
          BottomNavigationBarItem(
              icon: const Icon(Icons.home_rounded), label: l10n.translate('app_name')), // Or 'home'
          BottomNavigationBarItem(
              icon: const Icon(Icons.bar_chart_rounded), label: l10n.translate('stats')),
          BottomNavigationBarItem(
              icon: const Icon(Icons.person_rounded), label: l10n.translate('profile')),
          BottomNavigationBarItem(
              icon: const Icon(Icons.settings_rounded), label: l10n.translate('settings')),
        ],
      ),
    );
  }
}

class HomeContent extends StatelessWidget {
  const HomeContent({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<TransactionProvider>(context);
    final authProvider = Provider.of<AuthProvider>(context);
    final currency = NumberFormat.simpleCurrency(locale: 'en_US');
    final isDark = Theme.of(context).brightness == Brightness.dark;

    final l10n = AppLocalizations.of(context)!;

    return SingleChildScrollView(
      physics: const BouncingScrollPhysics(),
      child: Column(
        children: [

          _buildHeader(context, provider, authProvider, currency),


          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              children: [
                Expanded(
                    child: _buildActionButton(
                        context,
                        Icons.add_box_rounded,
                        l10n.translate('add_transaction').replaceFirst(' ', '\n'),
                        () => Navigator.push(
                            context,
                            MaterialPageRoute(
                                builder: (_) =>
                                    const AddTransactionScreen())))),
                const SizedBox(width: 16),
                Expanded(
                    child: _buildActionButton(
                        context,
                        Icons.history_rounded,
                        l10n.translate('transactions').replaceFirst(' ', '\n'), // Or 'history'
                        () => Navigator.push(
                            context,
                            MaterialPageRoute(
                                builder: (_) =>
                                    const AllTransactionsScreen())))),
              ],
            ),
          ),


          _buildRecentSection(context, provider, currency, isDark),


          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 30),
            child: Row(
              children: [
                Expanded(
                    child: _buildBigCard(
                        context,
                        Icons.account_balance_wallet_rounded,
                        l10n.translate('budget'),
                        () => Navigator.push(
                            context,
                            MaterialPageRoute(
                                builder: (_) => const BudgetScreen())))),
                const SizedBox(width: 16),
                Expanded(
                    child: _buildBigCard(
                        context,
                        Icons.category_rounded,
                        l10n.translate('categories'),
                        () => Navigator.push(
                            context,
                            MaterialPageRoute(
                                builder: (_) => const CategoriesScreen())))),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeader(BuildContext context, TransactionProvider provider,
      AuthProvider authProvider, NumberFormat currency) {
    final l10n = AppLocalizations.of(context)!;

    return Container(
      padding: const EdgeInsets.fromLTRB(24, 60, 24, 30),
      decoration: const BoxDecoration(
        color: Color(0xFF2E7D32),
        borderRadius: BorderRadius.vertical(bottom: Radius.circular(32)),
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text("${l10n.translate('welcome')},",
                      style: const TextStyle(color: Colors.white70, fontSize: 14)),
                  Text(authProvider.username ?? "User",
                      style: const TextStyle(
                          color: Colors.white,
                          fontSize: 20,
                          fontWeight: FontWeight.bold)),
                ],
              ),
              IconButton(
                icon: const Icon(Icons.notifications_none_rounded,
                    color: Colors.white),
                onPressed: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (_) => const NotificationScreen())),
              ),
            ],
          ),
          const SizedBox(height: 24),
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: const Color(0xFF43A047),
              borderRadius: BorderRadius.circular(24),
              boxShadow: [
                BoxShadow(
                    color: Colors.black12,
                    blurRadius: 10,
                    offset: const Offset(0, 5))
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(l10n.translate('total_balance'),
                    style: const TextStyle(color: Colors.white70, fontSize: 13)),
                const SizedBox(height: 4),
                FittedBox(
                  child: Text(currency.format(provider.totalBalance),
                      style: const TextStyle(
                          color: Colors.white,
                          fontSize: 34,
                          fontWeight: FontWeight.bold)),
                ),
                const SizedBox(height: 20),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    _buildBalanceIndicator(Icons.arrow_downward, l10n.translate('expense'),
                        provider.totalExpense, Colors.redAccent),
                    _buildBalanceIndicator(Icons.arrow_upward, l10n.translate('income'),
                        provider.totalIncome, Colors.lightGreenAccent),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBalanceIndicator(
      IconData icon, String label, double amount, Color color) {
    return Row(
      children: [
        Icon(icon, color: color, size: 18),
        const SizedBox(width: 8),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label,
                style: const TextStyle(color: Colors.white70, fontSize: 12)),
            Text("\$${amount.toStringAsFixed(0)}",
                style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                    fontSize: 15)),
          ],
        )
      ],
    );
  }

  Widget _buildRecentSection(BuildContext context, TransactionProvider provider,
      NumberFormat currency, bool isDark) {
    final l10n = AppLocalizations.of(context)!;
    final transactions = provider.transactions.take(3).toList();
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(l10n.translate('recent_transactions'),
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              TextButton(
                onPressed: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (_) => const AllTransactionsScreen())),
                child: Text(l10n.translate('search'), // Or 'view all'
                    style: const TextStyle(color: Color(0xFF2E7D32))),
              ),
            ],
          ),
        ),
        if (transactions.isEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 20),
            child: Text(l10n.translate('no_transactions'),
                style: const TextStyle(color: Colors.grey)),
          )
        else
          ListView.builder(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: transactions.length,
            itemBuilder: (context, index) {
              final tx = transactions[index];
              final isExpense = tx.type == 'EXPENSE';
              return Card(
                elevation: 0,
                color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
                margin: const EdgeInsets.only(bottom: 12),
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(20)),
                child: ListTile(
                  onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                          builder: (_) =>
                              TransactionDetailScreen(transaction: tx))),
                  leading: CircleAvatar(
                    backgroundColor: isExpense
                        ? const Color(0xFFFFEBEE)
                        : const Color(0xFFE8F5E9),
                    child: Icon(isExpense ? Icons.south_west : Icons.north_east,
                        color: isExpense ? Colors.red : Colors.green, size: 20),
                  ),
                  title: Text(tx.category,
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                  subtitle: Text(DateFormat('dd MMM, yyyy').format(tx.date)),
                  trailing: Text(
                    "${isExpense ? '-' : '+'}${currency.format(tx.amount)}",
                    style: TextStyle(
                        color: isExpense ? Colors.red : Colors.green,
                        fontWeight: FontWeight.bold,
                        fontSize: 16),
                  ),
                ),
              );
            },
          ),
      ],
    );
  }

  Widget _buildActionButton(
      BuildContext context, IconData icon, String label, VoidCallback onTap) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
          borderRadius: BorderRadius.circular(24),
          boxShadow: [
            BoxShadow(
                color: Colors.black.withOpacity(0.04),
                blurRadius: 10,
                offset: const Offset(0, 4))
          ],
        ),
        child: Row(
          children: [
            Icon(icon, color: const Color(0xFF2E7D32)),
            const SizedBox(width: 12),
            Flexible(
                child: Text(label,
                    style: const TextStyle(
                        fontWeight: FontWeight.bold, fontSize: 13))),
          ],
        ),
      ),
    );
  }

  Widget _buildBigCard(
      BuildContext context, IconData icon, String label, VoidCallback onTap) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 110,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
          borderRadius: BorderRadius.circular(24),
          boxShadow: [
            BoxShadow(
                color: Colors.black.withOpacity(0.04),
                blurRadius: 10,
                offset: const Offset(0, 4))
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Icon(icon, color: const Color(0xFF2E7D32), size: 32),
            Text(label,
                style:
                    const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          ],
        ),
      ),
    );
  }
}
