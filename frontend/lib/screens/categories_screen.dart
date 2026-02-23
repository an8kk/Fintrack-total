import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/category_model.dart';
import '../providers/category_provider.dart';
import '../providers/auth_provider.dart';
import '../l10n/app_localizations.dart';
import '../utils/constants.dart';

class CategoriesScreen extends StatelessWidget {
  const CategoriesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final provider = Provider.of<CategoryProvider>(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.translate('categories')),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.add_rounded),
            onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (_) => const CreateCategoryScreen())),
          )
        ],
      ),
      body: provider.categories.isEmpty
          ? Center(
              child: Text(l10n.translate('no_categories'),
                  style: const TextStyle(color: Colors.grey)))
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: provider.categories.length,
              itemBuilder: (ctx, i) {
                final cat = provider.categories[i];
                final Color catColor = Color(int.parse(
                    cat.color.replaceAll('#', '0xFF')));

                return Card(
                  elevation: 0,
                  color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                      side: BorderSide(
                          color:
                              isDark ? Colors.white10 : Colors.grey.shade100)),
                  margin: const EdgeInsets.only(bottom: 12),
                  child: ListTile(
                    leading: CircleAvatar(
                      backgroundColor: catColor.withValues(alpha: 0.15),
                      child: Icon(
                        provider.getIconData(cat.icon),
                        color: catColor,
                      ),
                    ),
                    title: Text(cat.name,
                        style: const TextStyle(fontWeight: FontWeight.bold)),
                     subtitle: Text(
                      cat.type == 'EXPENSE'
                           ? "${l10n.translate('budget_limit')}${cat.budgetLimit}"
                          : l10n.translate('income_source'),
                      style: const TextStyle(fontSize: 12),
                    ),
                    trailing: IconButton(
                      icon: const Icon(Icons.delete_sweep_rounded,
                          color: Colors.redAccent),
                      onPressed: () => _confirmDelete(context, provider, cat),
                    ),
                  ),
                );
              },
            ),
    );
  }

  void _confirmDelete(
      BuildContext context, CategoryProvider provider, CategoryModel cat) {
    final l10n = AppLocalizations.of(context)!;
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.translate('delete_confirm_title')),
        content: Text(
            l10n.translate('delete_confirm_msg').replaceAll('{name}', cat.name)),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx), child: Text(l10n.translate('cancel'))),
          TextButton(
              onPressed: () {
                provider.deleteCategory(cat.id!);
                Navigator.pop(ctx);
              },
              child:
                  Text(l10n.translate('delete'), style: const TextStyle(color: Colors.red))),
        ],
      ),
    );
  }
}

class CreateCategoryScreen extends StatefulWidget {
  const CreateCategoryScreen({super.key});

  @override
  State<CreateCategoryScreen> createState() => _CreateCategoryScreenState();
}

class _CreateCategoryScreenState extends State<CreateCategoryScreen> {
  final _nameController = TextEditingController();
  final _limitController = TextEditingController(text: "0");
  String _selectedIcon = "fastfood";
  String _selectedColor = "0xFF2196F3";
  String _type = "EXPENSE";

  final List<String> _icons = [
    "fastfood",
    "directions_bus",
    "shopping_bag",
    "home",
    "work",
    "movie",
    "local_hospital",
    "attach_money"
  ];
  final List<String> _colors = [
    "0xFF2196F3",
    "0xFF9C27B0",
    "0xFFE91E63",
    "0xFFFF9800",
    "0xFF4CAF50",
    "0xFF607D8B"
  ];

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
          title: Text(l10n.translate('new_category')),
          backgroundColor: AppColors.primary,
          foregroundColor: Colors.white),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
                controller: _nameController,
                style: TextStyle(color: isDark ? Colors.white : Colors.black),
                decoration: InputDecoration(
                    labelText: l10n.translate('name'),
                    filled: true,
                    fillColor: isDark ? Colors.white10 : Colors.grey[100],
                    border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: BorderSide.none))),
            const SizedBox(height: 20),
            Text(l10n.translate('category_type'),
                style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 10),
            Row(
              children: [
                _typeChip(l10n.translate('expense'), "EXPENSE"),
                const SizedBox(width: 12),
                _typeChip(l10n.translate('income'), "INCOME"),
              ],
            ),
            const SizedBox(height: 20),
            if (_type == 'EXPENSE') ...[
              TextField(
                  controller: _limitController,
                  keyboardType: TextInputType.number,
                  style: TextStyle(color: isDark ? Colors.white : Colors.black),
                  decoration: InputDecoration(
                      labelText: l10n.translate('budget_limit_label'),
                      filled: true,
                      fillColor: isDark ? Colors.white10 : Colors.grey[100],
                      border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: BorderSide.none))),
              const SizedBox(height: 20),
            ],
            Text(l10n.translate('select_color'),
                style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: _colors
                  .map((c) => GestureDetector(
                        onTap: () => setState(() => _selectedColor = c),
                        child: CircleAvatar(
                          backgroundColor: Color(int.parse(c)),
                          radius: 22,
                          child: _selectedColor == c
                              ? const Icon(Icons.check, color: Colors.white)
                              : null,
                        ),
                      ))
                  .toList(),
            ),
            const SizedBox(height: 24),
            Text(l10n.translate('select_icon'),
                style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            Wrap(
              spacing: 10,
              children: _icons
                  .map((icon) => ChoiceChip(
                        label: Icon(
                            Provider.of<CategoryProvider>(context,
                                    listen: false)
                                .getIconData(icon),
                            color: _selectedIcon == icon
                                ? Colors.white
                                : Colors.grey),
                        selected: _selectedIcon == icon,
                        selectedColor: AppColors.primary,
                        onSelected: (v) => setState(() => _selectedIcon = icon),
                      ))
                  .toList(),
            ),
            const SizedBox(height: 40),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () async {
                  if (_nameController.text.isEmpty) return;
                  await Provider.of<CategoryProvider>(context, listen: false)
                      .createCategory(
                          _nameController.text,
                          _selectedIcon,
                          _selectedColor,
                          double.tryParse(_limitController.text) ?? 0,
                          _type);
                  if (mounted) Navigator.pop(context);
                },
                style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 18),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16))),
                child: Text(l10n.translate('create'),
                    style:
                        const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              ),
            )
          ],
        ),
      ),
    );
  }

  Widget _typeChip(String label, String value) {
    final isSelected = _type == value;
    return ChoiceChip(
      label: Text(label),
      selected: isSelected,
      selectedColor: AppColors.primary,
      labelStyle: TextStyle(color: isSelected ? Colors.white : Colors.grey),
      onSelected: (v) => setState(() => _type = value),
    );
  }
}
