package com.example.data

object SampleDatasets {
    const val SALES_CSV = """Order ID,Product,Category,Sales ($),Quantity,Profit ($),Rating
1001,MacBook Pro,Electronics,1999.99,1,450.00,4.8
1002,iPad Air,Electronics,599.99,2,120.00,4.5
1003,Leather Jacket,Apparel,120.00,3,40.00,4.2
1004,Wireless Charger,Accessories,29.99,10,15.00,4.0
1005,Running Shoes,Apparel,89.99,2,25.00,4.6
1006,Desk Lamp,Home,45.00,4,12.00,4.3
1007,Ergonomic Chair,Furniture,249.99,1,60.00,4.7
1008,Coffee Mug,Home,12.50,15,6.00,4.1
1009,Bluetooth Speaker,Electronics,79.99,3,24.00,4.4
1010,Wool Socks,Apparel,15.00,5,5.00,4.5"""

    const val EMPLOYEE_CSV = """Employee ID,Name,Department,Years of Exp,Monthly Salary ($),Evaluation Score
EMP001,John Doe,Engineering,5,8500.00,4.7
EMP002,Jane Smith,Marketing,3,6200.00,4.3
EMP003,Alex Rivera,Sales,8,9500.00,4.8
EMP004,Lisa Chen,Design,2,5800.00,4.0
EMP005,Michael Scott,HR,10,7500.00,3.2
EMP006,David Miller,Engineering,7,9200.00,4.6
EMP007,Sarah Connor,Security,12,11000.00,4.9
EMP008,Peter Parker,Marketing,1,4500.00,4.5
EMP009,Clark Kent,HR,4,6500.00,4.2
EMP010,Bruce Wayne,Executive,15,25000.00,5.0"""

    const val FITNESS_CSV = """Date,Steps,Active Minutes,Sleep Hours,Calories Burned,Mood
2026-06-17,8500,45,7.2,2100,Happy
2026-06-18,12000,65,8.0,2450,Energetic
2026-06-19,4500,20,6.5,1800,Tired
2026-06-20,10200,50,7.5,2300,Calm
2026-06-21,15400,90,8.2,2800,Thrilled
2026-06-22,6000,30,7.0,1950,Focused
2026-06-23,11000,55,7.8,2400,Happy"""

    val samples = listOf(
        SampleDataInfo("Tech Product Sales.csv", SALES_CSV),
        SampleDataInfo("Employee Payroll & HR.csv", EMPLOYEE_CSV),
        SampleDataInfo("Fitness Tracker Daily.csv", FITNESS_CSV)
    )
}

data class SampleDataInfo(val name: String, val csvContent: String)
