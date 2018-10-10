# Design and implementation of an application based on Tango’s augmented reality technology
This project is part of my Thesis in developing an app with Google Tango. It mainly uses the [Depth Perception API](https://web.archive.org/web/20170326060429/https://developers.google.com/tango/overview/depth-perception)

## Thesis Chapters
1. Introduction
2. Augmented Reality
3. Tango Technology
4. App development with Tango
5. App implementation - Library(Bookcase) Recognition
6. Advantages - Disadvantages - Problems
7. Conclusions - Future Work
8. Appendix

## Information
My thesis contains an app implementation with Tango. We implemented an app that scans a library (bookcase and books).  
Analytically, we have three functions:
1. **Scan Library** - User gives input (Points at the screen) and we calculate the width and height of library
2. **Analyze Library** - Based on previous measurements we render the points of the library within it's limits
  **Important**: Due to the need of different Rajawali versions this version comes with the second app of this project
3. **Scan book** - With this function we have this flow
    - Scan Barcode
    - Measure book (same as Scan Library)
    - Check the DB for the Barcode value. If we can find it in the local DB we calculate the accuracy of the measurements.
    If not we retrieve books with similar sizes to calculate the accuracy.

## Deployment
For Scanning app you will need to set a firebase app. More info here: [https://firebase.google.com/docs/android/setup](https://firebase.google.com/docs/android/setup)  
Also you have to scan a barcode to enter the app. I have a default value of: "ABC-abc-1234".  
You can change this barcode value from BarcodeScanningProcessor.java.


### Features
- SQLite Database with [Content Provider](https://developer.android.com/guide/topics/providers/content-providers)
- Barcode scanner with [MLKit](https://developers.google.com/ml-kit/)
- Google Tango
- [Rajawali](https://github.com/Rajawali/Rajawali) OpenGL ES 2.0/3.0 Engine


### Presentation
You can find the thesis presentation [here](https://github.com/nkarampi/GoogleTangoThesis/Presentation)  
*Currently the presentation's language is Greek. For a translation you can contact me.*


### Resources
**Examples**  
https://github.com/googlearchive/tango-examples-java

**Documentation (Archived)**  
https://web.archive.org/web/20170714191103/https://developers.google.com/tango/apis/java/

**Book**  
Augmented Reality: Principles and Practice Book by Dieter Schmalstieg and Tobias Höllerer

**Papers**  
- Rafael Roberto, João Paulo Lima, Thúlio Araújo, Veronica Teichrieb. Evaluation of Motion Tracking and Depth Sensing Accuracy of the Tango Tablet. 2016.

- Prof. Dr. Eberhard Gülch. Investigations on Google Tango Development Kit for Personal Indoor Mapping
