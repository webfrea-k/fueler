package com.example.fueler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.android.gms.ads.MobileAds
import com.soywiz.klock.DateTime
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.math.roundToInt
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class MainActivity : ComponentActivity() {

    private var interstitialAd: InterstitialAd? = null
    private var interstitialAdShown: Boolean = false
    private var interstitialAdLoaded: Boolean = false

    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val adRequest: AdRequest = AdRequest.Builder().build()

        MobileAds.initialize(this) {
            InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    interstitialAdLoaded = true
                    interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() { /*NO OP*/ }
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                }
            })
        }

        setContent {
            val pages = listOf("LOGS","STATS")
            val pagerState = rememberPagerState()
            val scope = rememberCoroutineScope()
            val showAddEntry = remember { mutableStateOf(false) }
            val entries = DataStore.read(this@MainActivity).collectAsState(emptyList())
            Scaffold(
                floatingActionButtonPosition = FabPosition.End,
                floatingActionButton = {
                    FloatingActionButton(onClick = { showAddEntry.value = !showAddEntry.value }) {
                        Icon(Icons.Filled.Add,"")
                    }
                },
                content = {
                    Column {
                        TabRow(selectedTabIndex = pagerState.currentPage) {
                            pages.forEachIndexed { index, title ->
                                Tab(
                                    text = { Text(title) },
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        scope.launch { pagerState.scrollToPage(index) }
                                    },
                                )
                            }
                        }
                        HorizontalPager(
                            count = pages.size,
                            state = pagerState,
                        ) { page ->
                            when(page) {
                                0 -> FuelEntries(items = entries.value)
                                1 -> StatisticsEntries(items = entries.value)
                            }
                        }
                    }
                })
            if (showAddEntry.value) {
                AddEntryDialog(showAddEntry)
            }
            if (pagerState.currentPage == 1) {
                showInterstitialAdIfRequired()
            }
        }
    }

    private fun showInterstitialAdIfRequired() {
        if (!interstitialAdShown && interstitialAdLoaded) {
            interstitialAd?.show(this@MainActivity)
            interstitialAdShown = true
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun FuelEntries(items: List<FuelEntry>) {
        val byMonths = items.groupBy { DateTime.fromUnix(it.timestamp).month }
        LazyColumn(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            byMonths.forEach { (date, fuelItems) ->
                val year = DateTime.fromUnix(fuelItems.first().timestamp).yearInt
                stickyHeader {
                    Text(text = "$year ${date.localName}", modifier = Modifier
                        .background(color = colorResource(id = R.color.teal_200))
                        .fillMaxWidth()
                        .padding(all = 5.dp))
                }
                items(fuelItems.count()) {
                    FuelEntryItem(item = fuelItems[it], isGray = it % 2 == 0)
                }
                item(key = "$date $year") {
                    BannerAdView()
                }
            }
        }
    }

    @Composable
    private fun FuelEntryItem(item: FuelEntry, isGray: Boolean) {
        val litersFormat = DecimalFormat("#.##")
        Column(modifier = Modifier.background(if (isGray) Color.LightGray else Color.White)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)) {
                Text(text = "Date: ".plus(DateTime.fromUnix(item.timestamp).format("dd.MM.yyyy")))
                Text(text = "Amount: ${item.amountOfLiters} liters")
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)) {
                Text(text = "Odometer: ${item.odometer} km")
                Text(text = "Price: ${(item.amountOfLiters * item.priceOfLiter).toInt()}€ (${litersFormat.format(item.priceOfLiter)} €/L) ")
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun StatisticsEntries(items: List<FuelEntry>) {
        val byYears = items.groupBy { DateTime.fromUnix(it.timestamp).yearInt }
        LazyColumn(horizontalAlignment = Alignment.Start, modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()) {
            byYears.forEach { (date, fuelItems) ->
                stickyHeader {
                    Text(text = date.toString(), modifier = Modifier
                        .background(color = colorResource(id = R.color.teal_200))
                        .fillMaxWidth()
                        .padding(all = 5.dp))
                }
                item {
                    YearlyStatsEntryItem(items = fuelItems)
                }
            }
        }
    }

    @Composable
    private fun YearlyStatsEntryItem(items: List<FuelEntry>) {
        val priceFormat = DecimalFormat("#.##")
        Column(modifier = Modifier.padding(5.dp)) {
            val monthsInYear = items.groupBy { DateTime.fromUnix(it.timestamp).month }.keys.count() //cannot be 12, as user may start inserting in the middle of the year
            val kilometersMadeInYear = items.maxOf { it.odometer }.minus(items.minOf { it.odometer }).toDouble()
            val litersTankedInYear = items.sumOf { it.amountOfLiters }.toDouble()
            val consumption = priceFormat.format(litersTankedInYear.div(kilometersMadeInYear).times(100))
            Text(text = "Avg. consumption: $consumption L/100km", fontSize = 20.sp)
            Text(text = "Avg. monthly cost: ${items.sumOf { it.amountOfLiters * it.priceOfLiter }.div(monthsInYear).roundToInt()} €", fontSize = 20.sp)
            Text(text = "Avg. monthly distance: ${kilometersMadeInYear.div(monthsInYear).roundToInt()} km", fontSize = 20.sp)
        }
    }

    @Composable
    private fun AddEntryDialog(showAddEntry: MutableState<Boolean>) {
        val scope = rememberCoroutineScope()
        Dialog(onDismissRequest = { showAddEntry.value = false } ) {
            val liters = remember { mutableStateOf("") }
            val odometer = remember { mutableStateOf("") }
            val pricePerLiter = remember { mutableStateOf("") }
            Card {
                Column(modifier = Modifier.padding(10.dp)) {
                    TextField(label = { Text("Odometer: ") }, value = odometer.value, onValueChange = { odometer.value = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    TextField(label = { Text("Liters: ") }, value = liters.value, onValueChange = { liters.value = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    TextField(label = { Text("Price per liter: ") }, value = pricePerLiter.value, onValueChange = { pricePerLiter.value = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Button(
                        modifier = Modifier.padding(top = 10.dp),
                        enabled = odometer.value.toLongOrNull() != null && liters.value.toLongOrNull() != null && pricePerLiter.value.replace(",", ".").toDoubleOrNull() != null,
                        onClick = {
                            scope.launch {
                                DataStore.write(
                                    context = this@MainActivity,
                                    value = FuelEntry(
                                        timestamp = DateTime.nowUnixLong(),
                                        odometer = odometer.value.toLong(),
                                        amountOfLiters = liters.value.toLong(),
                                        priceOfLiter = pricePerLiter.value.replace(",",".").toDoubleOrNull() ?: 1.0
                                    )
                                )
                            }
                            showAddEntry.value = false
                        }
                    ) { Text(text = "ADD") }
                }
            }
        }
    }
}