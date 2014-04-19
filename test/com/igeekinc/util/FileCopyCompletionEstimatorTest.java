/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * @Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.@
 */
 
package com.igeekinc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import com.igeekinc.junitext.iGeekTestCase;

public class FileCopyCompletionEstimatorTest extends iGeekTestCase
{
    /*
    public void testBasic()
    {
        int kFileSize = 1024;
        int kBytesPerMS = 1024;
        int kNumFiles = 10000;
        int kFileOverheadTime = 10;
        
        int kTotalTimePerFile = kFileSize/kBytesPerMS;
        
        int totalTime = (kTotalTimePerFile * kNumFiles) + (kFileOverheadTime * kNumFiles);  // As calculated 
        
        FileCopyCompletionEstimator testEstimator = new FileCopyCompletionEstimator();
        testEstimator.setTotalBytesToBeCopied(kFileSize*kNumFiles);
        testEstimator.setTotalFilesToBeCopied(kNumFiles);
        long startTime = System.currentTimeMillis();    // Just to get a reasonable timestamp
        
        long expectedEndTime = startTime + totalTime;
        long simulatedTime = startTime;
        
        int numUncalculated = 0, numInBounds = 0, numOutOfBounds = 0;
        for (int curFileNum = 0; curFileNum < kNumFiles; curFileNum++)
        {
            testEstimator.startingFile(simulatedTime);
            simulatedTime+=kFileOverheadTime;
            testEstimator.startingCopying(simulatedTime, kFileOverheadTime);
            simulatedTime += kTotalTimePerFile;
            testEstimator.copyUpdate(simulatedTime, kFileSize);
            testEstimator.finishedCopying(simulatedTime);
            testEstimator.finishFile(simulatedTime);
            
            long estimatedFinishTime = testEstimator.getEstimatedCompletionTime();
            if (estimatedFinishTime > -1)
            {
                if (Math.abs(estimatedFinishTime - expectedEndTime) > totalTime/10)
                    numOutOfBounds ++;
                else
                    numInBounds ++;
            }
            else
            {
                numUncalculated++;
            }
        }
        System.out.println("Num inbounds:"+numInBounds+" Num out of bounds:"+numOutOfBounds+" Num uncalculated: "+numUncalculated);
    }
    
    public void testBigFiles()
    {
        int kFileSize = 1024;
        int kBigFileSize = 1024*1024*1024;  // 1GB should push us beyond the sample queue
        int kBytesPerMS = 1024;
        int kNumBigFileIOs = 1024;
        int kBigFileModulo = 1000;
        int kNumFiles = 10000;
        int kFileOverheadTime = 10;
        
        int kTotalTimePerFile = kFileSize/kBytesPerMS;
        int kBigTotalTimePerFile = kBigFileSize/kBytesPerMS;
        
        int numBigFiles = kNumFiles/kBigFileModulo;
        int numSmallFiles = kNumFiles - numBigFiles;
        int totalTime = (kTotalTimePerFile * numSmallFiles) + (kBigTotalTimePerFile * numBigFiles) + (kFileOverheadTime * kNumFiles);  // As calculated 
        
        FileCopyCompletionEstimator testEstimator = new FileCopyCompletionEstimator();
        testEstimator.setTotalBytesToBeCopied((long)kFileSize*(long)numSmallFiles + (long)kBigFileSize * (long)numBigFiles);
        testEstimator.setTotalFilesToBeCopied(kNumFiles);
        long startTime = System.currentTimeMillis();    // Just to get a reasonable timestamp
        
        long expectedEndTime = startTime + totalTime;
        long simulatedTime = startTime;
        
        int numUncalculated = 0, numInBounds = 0, numOutOfBounds = 0;
        for (int curFileNum = 0; curFileNum < kNumFiles; curFileNum++)
        {
            testEstimator.startingFile(simulatedTime);
            simulatedTime+=kFileOverheadTime;
            testEstimator.startingCopying(simulatedTime, kFileOverheadTime);
            
            if (curFileNum % kBigFileModulo == 0)
            {
                long bytesPerCall = kBigFileSize/ kNumBigFileIOs;
                long timePerCopy = bytesPerCall/kBytesPerMS;
                for (int curCopy = 0; curCopy < kNumBigFileIOs; curCopy++)
                {
                    simulatedTime += timePerCopy;
                    testEstimator.copyUpdate(simulatedTime, bytesPerCall);
                }
            }
            else
            {
                simulatedTime += kTotalTimePerFile;
                testEstimator.copyUpdate(simulatedTime, kFileSize);
            }
            testEstimator.finishedCopying(simulatedTime);
            testEstimator.finishFile(simulatedTime);
            
            long estimatedFinishTime = testEstimator.getEstimatedCompletionTime();
            if (estimatedFinishTime > -1)
            {
                long timeError = estimatedFinishTime - expectedEndTime;
                if (Math.abs(timeError) > totalTime/10)
                {
                    System.out.println("Out of bounds: file num = "+curFileNum+" timeError = "+timeError);
                    numOutOfBounds ++;
                }
                else
                {
                    numInBounds ++;
                }
            }
            else
            {
                numUncalculated++;
            }
        }
        System.out.println("Num inbounds:"+numInBounds+" Num out of bounds:"+numOutOfBounds+" Num uncalculated: "+numUncalculated);
    }
    */
    abstract class Sample
    {
        long sampleTime;
        
        Sample(long sampleTime)
        {
            this.sampleTime = sampleTime;
        }
        abstract void updateEstimator(FileCopyCompletionEstimator estimator);
    }
    
    class StartingFileSample extends Sample
    {
        long bytesToCopy;
        
        StartingFileSample(long sampleTime, long bytesToCopy)
        {
            super(sampleTime);
            this.bytesToCopy = bytesToCopy;
        }
        void updateEstimator(FileCopyCompletionEstimator estimator)
        {
            estimator.startingFile(sampleTime, bytesToCopy);
        }
    }
    
    class StartingCopySample extends Sample
    {
         StartingCopySample(long sampleTime)
        {
            super(sampleTime);
        }
        
        void updateEstimator(FileCopyCompletionEstimator estimator)
        {
            estimator.startingCopying(sampleTime);
        }
    }
    
    class UpdateCopySample extends Sample
    {
        long bytesCopiedThisUpdate;
        
        UpdateCopySample(long sampleTime, long bytesCopiedThisUpdate)
        {
            super(sampleTime);
            this.bytesCopiedThisUpdate = bytesCopiedThisUpdate;
        }
        
        void updateEstimator(FileCopyCompletionEstimator estimator)
        {
            estimator.copyUpdate(sampleTime, bytesCopiedThisUpdate);
        }
    }
    
    class FinishedCopySample extends Sample
    {
        FinishedCopySample(long sampleTime)
        {
            super(sampleTime);
        }
        void updateEstimator(FileCopyCompletionEstimator estimator)
        {
            estimator.finishedCopying(sampleTime);
        }
    }
    
    public void testRealData1()
    throws Exception
    {
        File testDataDir = new File("testdata/FileCopyCompletionEstimatorTestData");
        File [] testFiles = testDataDir.listFiles(new FileFilter() {
            public boolean accept(File pathname)
            {
                if (pathname.getName().endsWith(".data"))
                    return true;
                return false;
            }
        });
        for (File curTestFile:testFiles)
        {
            BufferedReader dataReader = new BufferedReader(new FileReader(curTestFile));

            FileCopyCompletionEstimator testEstimator = new FileCopyCompletionEstimator();
            String line1 = dataReader.readLine();
            setByteOrFileTotalFromLine(line1, testEstimator);
            String line2 = dataReader.readLine();
            setByteOrFileTotalFromLine(line2, testEstimator);
            String curLine = null;

            ArrayList<Sample>samples = new ArrayList<Sample>();
            while ((curLine = dataReader.readLine()) != null)
            {
                samples.add(createSampleFromLine(curLine));
            }

            long startTime = samples.get(0).sampleTime;
            long expectedEndTime = samples.get(samples.size()-1).sampleTime;
            long totalTime = expectedEndTime - startTime;

            int numOutOfBounds = 0, numInBounds = 0, numUncalculated = 0;
            int maxAbsPercentError = 0, totalAbsPercentError = 0;
            for (int curSampleNum = 0; curSampleNum < samples.size(); curSampleNum++)
            {
                Sample curSample = samples.get(curSampleNum);
                curSample.updateEstimator(testEstimator);
                long estimatedFinishTime = testEstimator.getEstimatedCompletionTime();

                if (estimatedFinishTime > -1)
                {
                    long timeError = estimatedFinishTime - expectedEndTime;
                    int percentError = (int)(((double)timeError/(double)totalTime) * 100.0);
                    int absPercentError = Math.abs(percentError);
                    if (absPercentError > 10)
                    {
                        //System.out.println("Out of bounds: sample num = "+curSampleNum+" expectedEndTime = "+new Date(expectedEndTime)+" estimatedEndTime = "+new Date(estimatedFinishTime)+" timeError = "+timeError+" % error ="+percentError + "b/s = "+(testEstimator.currentAverageBytesPerMS * 1000)+" fo = "+testEstimator.currentAverageFileOverhead+" ms");
                        numOutOfBounds ++;
                    }
                    else
                    {
                        numInBounds ++;
                    }
                    if (absPercentError > maxAbsPercentError)
                        maxAbsPercentError = absPercentError;
                    totalAbsPercentError+= absPercentError;
                }
                else
                {
                    numUncalculated++;
                }
            }
            System.out.println("File: "+curTestFile.getAbsolutePath());
            System.out.println("Num inbounds:"+numInBounds+" Num out of bounds:"+numOutOfBounds+" Num uncalculated: "+numUncalculated);
            System.out.println("Max % error = "+maxAbsPercentError+" average % error "+(totalAbsPercentError/samples.size()));
        }
    }
    
    private void setByteOrFileTotalFromLine(String line, FileCopyCompletionEstimator estimator)
    {
        StringTokenizer tokenizer = new StringTokenizer(line);
        String typeStr = tokenizer.nextToken();
        if (typeStr.equals("B"))
        {
            String totalBytesStr = tokenizer.nextToken();
            long totalBytes = Long.parseLong(totalBytesStr);
            estimator.setTotalBytesToBeCopied(totalBytes);
            return;
        }
        
        if (typeStr.equals("F"))
        {
            String totalFilesStr = tokenizer.nextToken();
            long totalFiles = Long.parseLong(totalFilesStr);
            estimator.setTotalFilesToBeCopied(totalFiles);
            return;
        }
        
        throw new IllegalArgumentException("'"+line+"' is not a B or F line");
    }
    
    Sample createSampleFromLine(String line)
    {
        StringTokenizer tokenizer = new StringTokenizer(line);
        String typeStr = tokenizer.nextToken();
        Sample returnSample = null;
        if (typeStr.equals("FS"))
        {
            long sampleTime = Long.parseLong(tokenizer.nextToken());
            long bytesToCopy = Long.parseLong(tokenizer.nextToken());
            returnSample = new StartingFileSample(sampleTime, bytesToCopy);
        }
        
        if (typeStr.equals("CB"))
        {
            long sampleTime = Long.parseLong(tokenizer.nextToken());

            returnSample = new StartingCopySample(sampleTime);
        }
        
        if (typeStr.equals("CF"))
        {
            long sampleTime = Long.parseLong(tokenizer.nextToken());
            returnSample = new FinishedCopySample(sampleTime);
        }
        
        if (typeStr.equals("CU"))
        {
            long sampleTime = Long.parseLong(tokenizer.nextToken());
            long bytesCopiedThisUpdate = Long.parseLong(tokenizer.nextToken());
            returnSample = new UpdateCopySample(sampleTime, bytesCopiedThisUpdate);
        }
        
        if (returnSample == null)
            throw new IllegalArgumentException("'"+line+"' does not begin with a recognized type");
        return returnSample;
    }
}
