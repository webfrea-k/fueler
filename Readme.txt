1. Build/Run instructions:
- Open the project in AndroidStudio ArcticFox (or newer) and run the project - it should open the app in emulator

2. How do you incorporate ads into the app’s flow? What are important factors here?
- Interstitial ads shouldn't break natural flow (for example in the process of entering new data)
- Banner ads are perfect for 'subtle messages' in the data list. You can put many items if the list is long, and most users will not argue that there is too much ads as they are not visible all at once. Still, it's best (imho) if only one banner ad is visible on the screen (it looks bad if there are more animated ads - each advertising it's own product on the same screen).
- I decided to show interstitial ad when users swipes or clicks on the STATS tab. People are usually perceptive to new products/ideas when they want to see the statistics (they have time and are curious). Displaying interstitial ad anywhere on the first screen would break the natural flow as you are probably in the car on the gas station when you are inserting new data into the app.
- Right placement and right frequency is something that shows over time, as there is no general rule that guarantees success.

3. What are possible approaches to control when the ads are shown or triggered in runtime, when the app is already published?
- The simplest approach would be A/B testing using Firebase. It's also helpful to send user-properties which are displayed as events and can be incorporated into A/B testing later on (level finished, level failed (reward), ...).

4. How would you assess whether the ads are negatively impacting your users?
- Users will let you know loud and clear in the Review section on Google Play
- You are losing audience once you have published version containing more ads
- Rating is falling