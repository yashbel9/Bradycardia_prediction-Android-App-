# Android Application for Bradycardia detection and prediction
In this project, we have developed an android application, that collects the ECG signal from the user and uses the data collected to detect the heart rate from the ECG signal detected. By constantly monitoring the heart rate, we ensure the users health and can possibly prevent various arrhythmia events. In our implementation, we attempt to detect and predict Bradycardia instants in the application.
The android application was implemented in the following steps:
1. Collection of data
2. Clean ECG data and implement peak detection 
3. Derive Heart Rate and plot variance vs time
4. Write Bradycardia Detection algorithm and label the data accordingly for the training data of Machine Learning Algorithm
5. Implement Android Application using SVM algorithm for detection of bradycardia using k-fold cross validation
6. Evaluate performance of the application and SVm algorithm
