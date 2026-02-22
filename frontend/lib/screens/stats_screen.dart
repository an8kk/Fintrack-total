import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/transaction_provider.dart';
import '../utils/constants.dart';

class StatsScreen extends StatelessWidget {
  const StatsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<TransactionProvider>(context);

    // Group transactions by category for the chart
    Map<String, double> dataMap = {};
    for (var tx in provider.transactions) {
      if (tx.type == 'EXPENSE') {
        dataMap[tx.category] = (dataMap[tx.category] ?? 0) + tx.amount;
      }
    }

    List<BarChartGroupData> barGroups = [];
    int index = 0;
    dataMap.forEach((key, value) {
      barGroups.add(
        BarChartGroupData(
          x: index,
          barRods: [
            BarChartRodData(
                toY: value,
                color: AppColors.primary,
                width: 16,
                borderRadius: BorderRadius.circular(4)),
          ],
        ),
      );
      index++;
    });

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text("Статистика расходов",
                style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
            const SizedBox(height: 30),
            Expanded(
              child: dataMap.isEmpty
                  ? const Center(child: Text("Нет данных о расходах"))
                  : BarChart(
                      BarChartData(
                        borderData: FlBorderData(show: false),
                        gridData: const FlGridData(show: false),
                        titlesData: FlTitlesData(
                          leftTitles: const AxisTitles(
                              sideTitles: SideTitles(
                                  showTitles: true, reservedSize: 40)),
                          bottomTitles: AxisTitles(
                            sideTitles: SideTitles(
                              showTitles: true,
                              getTitlesWidget: (double value, TitleMeta meta) {
                                if (value.toInt() < dataMap.keys.length) {
                                  return Padding(
                                    padding: const EdgeInsets.only(top: 8.0),
                                    child: Text(dataMap.keys
                                        .elementAt(value.toInt())
                                        .substring(0, 3)),
                                  );
                                }
                                return const Text('');
                              },
                            ),
                          ),
                          topTitles: const AxisTitles(
                              sideTitles: SideTitles(showTitles: false)),
                          rightTitles: const AxisTitles(
                              sideTitles: SideTitles(showTitles: false)),
                        ),
                        barGroups: barGroups,
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
