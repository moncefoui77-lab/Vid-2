package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.VidSmartGradient

@Composable
fun GuideScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ النص بنجاح!", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(VidSmartGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RocketLaunch,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "دليل تثبيت VidSmart على الهاتف مباشرة",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "خطوات سهلة ومبسطة للمبتدئين للحصول على ملف الـ APK وتثبيته على هاتفك الأندرويد بدون الحاجة إلى جهاز كمبيوتر إطلاقاً.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Method 1: Direct from AI Studio menu
        item {
            GuideStepCard(
                stepNumber = "1",
                title = "الطريقة الأولى (الأسهل والأسرع): من متصفح هاتفك في AI Studio",
                description = "يمكنك تصدير التطبيق كملف APK أو تنزيل ملف المشروع ZIP مباشرة من واجهة AI Studio في متصفحك:",
                steps = listOf(
                    "افتح قائمة الإعدادات (أيقونة الترس أو الثلاث نقاط ⚙️ في أعلى يمين واجهة المشروع).",
                    "اضغط على خيار «Export APK» أو «Download APK» أو «Export to ZIP».",
                    "سيتم بناء التطبيق وتنزيل ملف APK مباشرة في مجلد التنزيلات على هاتفك.",
                    "اضغط على ملف الـ APK المُنزل واضغط «تثبيت / Install»."
                )
            )
        }

        // Method 2: GitHub Actions (Free Cloud Builder)
        item {
            val workflowCode = """
name: Build VidSmart APK

on:
  push:
    branches: [ "main" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    - name: Build APK
      run: ./gradlew assembleDebug --stacktrace
    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: VidSmart-App
        path: app/build/outputs/apk/debug/app-debug.apk
            """.trimIndent()

            GuideStepCard(
                stepNumber = "2",
                title = "الطريقة الثانية: البناء السحابي المجاني عبر GitHub Actions",
                description = "إذا كنت تريد بناء الـ APK بنفسك مجاناً عبر السحابة باستخدام هاتفك فقط وبدون أي أجهزة:",
                steps = listOf(
                    "افتح موقع GitHub.com من متصفح هاتفك وقم بإنشاء مستودع جديد (New Repository) باسم vidsmart.",
                    "في AI Studio اضغط على زر «Push to GitHub» لربط ونقل الكود لمستودعك.",
                    "في GitHub على هاتفك، اضغط على «Add file» ثم «Create new file» واكتب المسار: .github/workflows/build.yml",
                    "انسخ الكود الجاهز أدناه وضعه في الملف ثم احفظ (Commit).",
                    "توجه إلى تبويب «Actions» وسترى البناء يعمل سحابياً! بعد دقيقتين ستجد ملف APK جاهزاً في قسم Artifacts للتنزيل على هاتفك فوراً!"
                ),
                codeSnippet = workflowCode,
                onCopyCode = { copyToClipboard("GitHub Workflow", workflowCode) }
            )
        }

        // Method 3: Security Settings on Phone
        item {
            GuideStepCard(
                stepNumber = "3",
                title = "السماح بتثبيت التطبيقات الخارجية (Unknown Sources)",
                description = "عند تثبيت أي ملف APK لأول مرة على أندرويد:",
                steps = listOf(
                    "ستظهر لك رسالة من نظام أندرويد: «لأسباب أمنية لا يُسمح بتثبيت تطبيقات من هذا المصدر».",
                    "اضغط على «الإعدادات / Settings» في الرسالة.",
                    "فعّل خيار «السماح من هذا المصدر / Allow from this source».",
                    "ارجع واضغط «تثبيت / Install» وسيفتح التطبيق على الفور بجميع ميزاته!"
                )
            )
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun GuideStepCard(
    stepNumber: String,
    title: String,
    description: String,
    steps: List<String>,
    codeSnippet: String? = null,
    onCopyCode: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(OrangePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            steps.forEachIndexed { idx, stp ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        color = OrangePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = stp,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }

            if (codeSnippet != null && onCopyCode != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF14131C),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GitHub Actions Workflow YAML",
                                fontSize = 12.sp,
                                color = PurpleAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = onCopyCode,
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ الكود", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = codeSnippet,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFFE0E0E0),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
