package com.example.myapplication;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.tabs.TabLayout;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
public class HistoryDayFragment extends Fragment {

    private TextView tvTotal, tvGoal, tvDateRange;
    private BarChart barChart;
    private int dailyGoal;
    private TabLayout tabLayout;
    private DatabaseHelper dbHelper;
    private List<Record> records;
    private RecyclerView recordRecyclerView;
    private Calendar calendar;
    private String currentPeriod = "DAY";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.history, container, false);

        tvTotal = view.findViewById(R.id.tvTotal);
        tvGoal = view.findViewById(R.id.tvGoal);
        tvDateRange = view.findViewById(R.id.tvDateRange); // Ensure this line is present
        FrameLayout chartContainer = view.findViewById(R.id.chartContainer);
        tabLayout = view.findViewById(R.id.tabLayout);
        recordRecyclerView = view.findViewById(R.id.recordRecyclerView);
        view.findViewById(R.id.ivDownload).setOnClickListener(v -> showDownloadOptionsDialog());
        dbHelper = new DatabaseHelper(requireContext());
        dailyGoal = dbHelper.getDailyGoal();
        calendar = Calendar.getInstance();
        // Apply the saved theme color
        applySavedThemeColor(view);

        // Create chart programmatically
        barChart = new BarChart(requireContext());
        chartContainer.addView(barChart, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setupChart(currentPeriod);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentPeriod = tab.getText().toString();
                setupChart(currentPeriod);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        view.findViewById(R.id.btnPrev).setOnClickListener(v -> {
            adjustDateRange(-1);
            setupChart(currentPeriod);
        });

        view.findViewById(R.id.btnNext).setOnClickListener(v -> {
            adjustDateRange(1);
            setupChart(currentPeriod);
        });

        // Load and display water intake records
        records = dbHelper.getAllRecords(); // Initialize records
        RecordAdapter adapter = new RecordAdapter(records, new RecordAdapter.OnRecordLongClickListener() {
            @Override
            public void onRecordLongClick(Record record, int position) {
                showEditDeleteDialog(record, position);
            }
        });
        recordRecyclerView.setAdapter(adapter);
        recordRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        return view;
    }
    // Apply the saved theme color in HistoryDayFragment
    private void applySavedThemeColor(View view) {
        SharedPreferences preferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        int savedColor = preferences.getInt("themeColor", ContextCompat.getColor(requireContext(), R.color.blue)); // Default color
        int lightColor = lightenColor(savedColor, 0.2f); // Calculate lighter shade

        // Apply base color to main layout
        View mainLayout = view.findViewById(R.id.main); // Ensure your main layout has this ID
        if (mainLayout != null) {
            mainLayout.setBackgroundColor(savedColor);
        }

        // Apply light color to ScrollView
        ScrollView scrollView = view.findViewById(R.id.scrollView); // Ensure your ScrollView has this ID
        if (scrollView != null) {
            scrollView.setBackgroundColor(lightColor);
        }

        // Apply base color to TabLayout
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        if (tabLayout != null) {
            tabLayout.setBackgroundColor(savedColor);
        }

    }

    // Utility method to lighten a color
    private int lightenColor(int color, float factor) {
        int r = (int) ((Color.red(color) * (1 - factor) + 255 * factor));
        int g = (int) ((Color.green(color) * (1 - factor) + 255 * factor));
        int b = (int) ((Color.blue(color) * (1 - factor) + 255 * factor));
        return Color.rgb(r, g, b);
    }
    private void setupChart(String period) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account == null) {
            Log.e("setupChart", "No Google account found");
            return;
        }
        String userId = account.getId();
        List<BarEntry> entries = new ArrayList<>();
        float total = 0;
        int goal = dailyGoal;
        SimpleDateFormat sdf;

        if (period.equals("DAY")) {
            sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String date = sdf.format(calendar.getTime());
            List<Integer> hourlyIntake = dbHelper.getWaterIntakeByDay(date, userId);
            for (int hour = 0; hour < 24; hour++) {
                float ml = hourlyIntake.get(hour);
                total += ml;
                entries.add(new BarEntry(hour, ml));
            }
            updateDateRangeDisplay("dd/MM/yyyy");
        } else if (period.equals("WEEK")) {
            // Get the date string in the desired format
            sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String date = sdf.format(calendar.getTime());

            // Determine the current week based on the day of the month
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
            int currentWeek = (dayOfMonth - 1) / 7 + 1;

            goal *= 7;
            List<Integer> weeklyIntake = dbHelper.getWaterIntakeByCustomWeek(date, userId);
            for (int week = 0; week < weeklyIntake.size(); week++) {
                float ml = weeklyIntake.get(week);
                total += ml;
                entries.add(new BarEntry(week, ml));
            }

            // Update the date range display with the correct week format
            updateDateRangeDisplayWeek(currentWeek, date);
        } else if (period.equals("MONTH")) {
            sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            String month = sdf.format(calendar.getTime());

            // Create a new Calendar instance and set it to the correct month and year
            Calendar tempCalendar = Calendar.getInstance();
            tempCalendar.setTime(calendar.getTime());
            int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH); // Correctly handles varying days

            goal *= daysInMonth;
            Log.d("HistoryDayFragment", month);
            List<Integer> monthlyIntake = dbHelper.getWaterIntakeByMonth(month, userId);

            // Ensure the list size matches the number of days in the month
            if (monthlyIntake.size() != daysInMonth) {
                Log.e("setupChart", "Mismatch in days: expected " + daysInMonth + ", got " + monthlyIntake.size());
                return; // Exit to avoid IndexOutOfBoundsException
            }

            for (int day = 0; day < daysInMonth; day++) {
                float ml = monthlyIntake.get(day);
                total += ml;
                entries.add(new BarEntry(day, ml));
            }
            updateDateRangeDisplay("MM / yyyy");
        }

        BarDataSet dataSet = new BarDataSet(entries, "Water Intake");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.setFitBars(true);
        barChart.setDrawValueAboveBar(true);
        barChart.getDescription().setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.RED);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(entries.size());
        xAxis.setValueFormatter(new IndexAxisValueFormatter(getLabels(period)));
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(entries.size() - 1);
        xAxis.setLabelRotationAngle(90);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(Color.RED);
        leftAxis.setGranularity(50f);
        leftAxis.setLabelCount(6, true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + " ml";
            }
        });

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        tvTotal.setText(getString(R.string.total_history) + (int) total + " ml");
        tvGoal.setText(getString(R.string.goal_history) + goal + " ml");

        barChart.setExtraOffsets(10, 10, 10, 10);
        barChart.invalidate();
    }
    private void updateDateRangeDisplayWeek(int week, String month) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM / yyyy", Locale.getDefault());
        String monthYear = sdf.format(calendar.getTime());
        tvDateRange.setText("Week " + week + " / " + monthYear);
    }
    private void updateDateRangeDisplay(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        tvDateRange.setText(sdf.format(calendar.getTime()));
    }

    private void adjustDateRange(int amount) {
        switch (currentPeriod) {
            case "DAY":
                calendar.add(Calendar.DAY_OF_MONTH, amount);
                break;
            case "WEEK":
                calendar.add(Calendar.WEEK_OF_YEAR, amount);
                break;
            case "MONTH":
                calendar.add(Calendar.MONTH, amount);
                break;
        }
    }

    private List<String> getLabels(String period) {
        List<String> labels = new ArrayList<>();
        if (period.equals("DAY")) {
            for (int i = 0; i < 24; i++) {
                labels.add(i + "h");
            }
        } else if (period.equals("WEEK")) {
            labels.add("Sun");
            labels.add("Mon");
            labels.add("Tue");
            labels.add("Wed");
            labels.add("Thu");
            labels.add("Fri");
            labels.add("Sat");
        } else if (period.equals("MONTH")) {
            int daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int i = 1; i <= daysInMonth; i++) {
                labels.add(String.valueOf(i));
            }
        }
        return labels;
    }

    private void showEditDeleteDialog(Record record, int position) {
        if (record == null) {
            Log.e("showEditDeleteDialog", "Record is null. Cannot proceed.");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit or Delete Record")
                .setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        Log.d("showEditDeleteDialog", "Edit option selected for record at position: " + position);
                        showEditRecordDialog(record, position);
                    } else {
                        Log.d("showEditDeleteDialog", "Delete option selected for record at position: " + position);
                        deleteRecord(record, position);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Log.d("showEditDeleteDialog", "User canceled the dialog.");
                    dialog.dismiss();
                })
                .show();
    }

    private void showEditRecordDialog(Record record, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_record, null);
        builder.setView(dialogView);

        TextView tvRecordAmount = dialogView.findViewById(R.id.tvRecordAmount);
        SeekBar seekBarRecordAmount = dialogView.findViewById(R.id.seekBarRecordAmount);

        // Set initial value
        int initialAmount = Integer.parseInt(record.getAmount().replace(" ml", ""));
        tvRecordAmount.setText("Selected: " + initialAmount + " ml");
        seekBarRecordAmount.setProgress(initialAmount);

        // Update TextView as SeekBar value changes
        seekBarRecordAmount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvRecordAmount.setText("Selected: " + progress + " ml");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No action needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No action needed
            }
        });

        builder.setPositiveButton("Save", (dialog, which) -> {
            int newAmount = seekBarRecordAmount.getProgress();
            updateRecord(String.valueOf(newAmount), position);
        }).setNegativeButton("Cancel", null).show();
    }

    private void updateRecord(String newAmount, int position) {
        Record record = records.get(position);
        String timestamp = record.getTimestamp();

        // Update the local database
        dbHelper.updateWaterIntakeRecord(timestamp, Integer.parseInt(newAmount));
        records.set(position, new Record(record.getId(), newAmount + " ml", timestamp));
        recordRecyclerView.getAdapter().notifyItemChanged(position);

        // Update Firebase
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
            if (account != null) {
                String userId = account.getId();
                SharedPreferences sharedPreferences = mainActivity.getSharedPreferencesInstance();
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.query(DatabaseHelper.TABLE_USER,
                                         new String[]{"gender", "height", "weight"},
                                         null,
                                         null,
                                         null,
                                         null,
                                         null);

                String gender = "unknown";
                double height = 0.0;
                double weight = 0.0;

                if (cursor.moveToFirst()) {
                    gender = cursor.getString(cursor.getColumnIndexOrThrow("gender"));
                    height = cursor.getDouble(cursor.getColumnIndexOrThrow("height"));
                    weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
                }
                cursor.close();

                mainActivity.uploadDatabase(userId, gender, height, weight, dailyGoal, new Runnable() {
                    @Override
                    public void run() {
                        Log.d("HistoryDayFragment", "Database uploaded successfully after update");
                    }
                });
            }
        }

        // Update the chart
        setupChart("DAY");
    }

    private void deleteRecord(Record record, int position) {
        if (position >= 0 && position < records.size()) {
            int recordId = record.getId(); // Use the unique ID of the record

            dbHelper.deleteWaterIntakeRecord(recordId); // Delete record by ID
            records.remove(position);
            recordRecyclerView.getAdapter().notifyItemRemoved(position);
            recordRecyclerView.getAdapter().notifyItemRangeChanged(position, records.size() - position);

            // Notify MainActivity to sync with Firebase
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.syncDeletedRecord(String.valueOf(recordId)); // Sync using the record ID

                // Call uploadDatabase to update Firebase
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
                if (account != null) {
                    String userId = account.getId();
                    SharedPreferences sharedPreferences = mainActivity.getSharedPreferencesInstance();
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    Cursor cursor = db.query(DatabaseHelper.TABLE_USER,
                                             new String[]{"gender", "height", "weight"},
                                             null,
                                             null,
                                             null,
                                             null,
                                             null);

                    String gender = "unknown";
                    double height = 0.0;
                    double weight = 0.0;

                    if (cursor.moveToFirst()) {
                        gender = cursor.getString(cursor.getColumnIndexOrThrow("gender"));
                        height = cursor.getDouble(cursor.getColumnIndexOrThrow("height"));
                        weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
                    }
                    cursor.close();
                    mainActivity.uploadDatabase(userId, gender, height, weight, dailyGoal, new Runnable() {
                        @Override
                        public void run() {
                            Log.d("HistoryDayFragment", "Database uploaded successfully after update");
                        }
                    });
                }
            }

            // Update the chart
            setupChart("DAY");
        } else {
            Log.e("HistoryDayFragment", "Invalid position: " + position);
        }
    }
    private void showDownloadOptionsDialog() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("Select Report Type");

                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_download_options, null);
                    builder.setView(dialogView);

                    TextView tvDateInput = dialogView.findViewById(R.id.tvDateInput); // Replace EditText with TextView
                    Spinner spReportType = dialogView.findViewById(R.id.spReportType);
                    Spinner spFileFormat = dialogView.findViewById(R.id.spFileFormat);

                    // Set up date picker
                    tvDateInput.setOnClickListener(v -> {
                        Calendar calendar = Calendar.getInstance();
                        String selectedReportType = spReportType.getSelectedItem().toString();

                        if (selectedReportType.equals("Day/Month/Year")) {
                            // Date picker for Day/Month/Year
                            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                                (view, year, month, dayOfMonth) -> {
                                    String selectedDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year);
                                    tvDateInput.setText(selectedDate);
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH));
                            datePickerDialog.show();

                        } else if (selectedReportType.equals("Week/Month/Year")) {
                            // Date picker for Week/Month/Year
                            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                                (view, year, month, dayOfMonth) -> {
                                    int week = (dayOfMonth - 1) / 7 + 1; // Calculate week number
                                    String selectedDate = String.format("week%d/%02d/%d", week, month + 1, year); // Use lowercase "week"
                                    tvDateInput.setText(selectedDate);
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH));
                            datePickerDialog.show();

                        } else if (selectedReportType.equals("Month/Year")) {
                            // Date picker for Month/Year
                            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                                (view, year, month, dayOfMonth) -> {
                                    String selectedDate = String.format("%02d/%d", month + 1, year);
                                    tvDateInput.setText(selectedDate);
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH));
                            datePickerDialog.show();
                        }
                    });

                    builder.setPositiveButton("OK", (dialog, which) -> {
                        String reportType = spReportType.getSelectedItem().toString();
                        String dateInput = tvDateInput.getText().toString();
                        String fileFormat = spFileFormat.getSelectedItem().toString();
                        generateReport(reportType, dateInput, fileFormat);
                    }).setNegativeButton("Cancel", null).show();
                }
    // HistoryDayFragment.java
    private void generateReport(String reportType, String dateInput, String fileFormat) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account != null) {
            String userId = account.getId();
            List<Integer> data = new ArrayList<>();
            switch (reportType) {
                case "Day/Month/Year":
                    data = dbHelper.getWaterIntakeByDayForDownload(userId, dateInput);
                    Log.d("Testday", "Data: " + data);
                    break;
                case "Week/Month/Year":
                    data = dbHelper.getWaterIntakeByWeekForDownload(userId, dateInput);
                    break;
                case "Month/Year":
                    data = dbHelper.getWaterIntakeByMonthForDownload(userId, dateInput);
                    break;
            }

            if (fileFormat.equals("Excel")) {
                exportToExcel(data, reportType, dateInput);
            } else if (fileFormat.equals("PDF")) {
                exportToPDF(data, reportType, dateInput);
            }
        } else {
            Log.e("HistoryDayFragment", "No Google account found");
        }
    }
    private void exportToExcel(List<Integer> data, String reportType, String dateInput) {
        Log.d("Test", "Exporting to Excel with data: " + data);
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Water Intake Report");

        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Time");
        headerRow.createCell(1).setCellValue("Water Intake (ml)");

        // Fill data rows
        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(getTimeLabel(reportType, i));
            row.createCell(1).setCellValue(data.get(i));
        }

        // Replace slashes in dateInput with underscores
        String sanitizedDateInput = dateInput.replace("/", "_");

        // Get the Download directory
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadDir, "WaterIntakeReport_" + sanitizedDateInput + ".xlsx");

        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
            Toast.makeText(requireContext(), "Excel export successful", Toast.LENGTH_SHORT).show();
            Log.d("Test", "Excel export successful: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Test", "Excel export failed", e);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void exportToPDF(List<Integer> data, String reportType, String dateInput) {
        Document document = new Document();
        try {
            // Replace slashes in dateInput with underscores
            String sanitizedDateInput = dateInput.replace("/", "_");

            // Get the Download directory
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadDir, "WaterIntakeReport_" + sanitizedDateInput + ".pdf");

            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Add title
            document.add(new Paragraph("Water Intake Report"));
            document.add(new Paragraph("Report Type: " + reportType));
            document.add(new Paragraph("Date: " + dateInput));
            document.add(new Paragraph(" ")); // Empty line

            // Create table
            PdfPTable table = new PdfPTable(2);
            table.addCell("Time");
            table.addCell("Water Intake (ml)");

            // Fill table with data
            for (int i = 0; i < data.size(); i++) {
                table.addCell(getTimeLabel(reportType, i));
                table.addCell(String.valueOf(data.get(i)));
            }

            document.add(table);
            Toast.makeText(requireContext(), "PDF export successful", Toast.LENGTH_SHORT).show();
            Log.d("Test", "PDF export successful: " + file.getAbsolutePath());
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
            Log.e("Test", "PDF export failed", e);
        } finally {
            document.close();
        }
    }
    private String getTimeLabel(String reportType, int index) {
        switch (reportType) {
            case "Day/Month/Year":
                return index + ":00";
            case "Week/Month/Year":
                return new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}[index];
            case "Month/Year":
                return String.valueOf(index + 1);
            default:
                return "";
        }
    }
}