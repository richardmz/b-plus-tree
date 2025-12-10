/**
 * Copyright 2025 Chen Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.richardmz.bplustree;

import java.util.ArrayList;
import java.util.List;

public class RandomDeleteTest
{
    private static BPlusTree<Integer, String> bPlusTree;

    public static void main(String[] args)
    {
        System.setErr(System.out);
        Logger logger = Logger.getInstance(Logger.Level.INFO);
        logger.start();

        try
        {
            bPlusTree = new BPlusTree<>(1024);
        }
        catch (DegreeTooSmallException e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        try
        {
            int number = 1000000;

            List<Integer> pickupInsertionList = new ArrayList<>(number);

            for (int i = 1; i <= number; i ++)
            {
                pickupInsertionList.add(i);
            }

            List<Integer> randomInsertionList = new ArrayList<>(number);

            logger.info("Generating random insertion list...");

            long generateRandomInsertStart = System.currentTimeMillis();

            while (randomInsertionList.size() < number)
            {
                int randomIndex = (int) (pickupInsertionList.size() * Math.random());
                randomInsertionList.add(pickupInsertionList.remove(randomIndex));
            }

            long generateRandomInsertEnd = System.currentTimeMillis();

            logger.info(String.format("Generating random insertion list used time: %d ms (%d minutes)", generateRandomInsertEnd - generateRandomInsertStart, (generateRandomInsertEnd - generateRandomInsertStart) / 60000));

            logger.info(String.format("randomInsertionList.size()： %d", randomInsertionList.size()));

            List<Integer> pickupDeletionList = new ArrayList<>(number);

            for (int i = 1; i <= number; i ++)
            {
                pickupDeletionList.add(i);
            }

            List<Integer> randomDeletionList = new ArrayList<>(number);

            long generateRandomDeleteStart = System.currentTimeMillis();

            logger.info("Generating random deletion list...");

            while (randomDeletionList.size() < number)
            {
                int randomIndex = (int) (pickupDeletionList.size() * Math.random());
                randomDeletionList.add(pickupDeletionList.remove(randomIndex));
            }

            long generateRandomDeleteEnd = System.currentTimeMillis();

            logger.info(String.format("Generating random deletion list used time: %d ms (%d minutes)", generateRandomDeleteEnd - generateRandomDeleteStart, (generateRandomDeleteEnd - generateRandomDeleteStart) / 60000));

            logger.info(String.format("randomDeletionList.size()： %d", randomDeletionList.size()));

            logger.info("Inserting...");

            long insertionStartTime = System.currentTimeMillis();

            for (int i = 0; i < number; i ++)
            {
                int num = randomInsertionList.get(i);
                long insertOneStarTime = System.currentTimeMillis();
                insert(num, String.valueOf(num));
                long insertOneEndTime = System.currentTimeMillis();
//                if (i % (number / 2 - 2) == 0)
//                {
//                    logger.info("i: " + i);
//                    logger.info(String.format("Insert one used time: %d ms", insertOneEndTime - insertOneStarTime));
//                }
            }

            long insertionEndTime = System.currentTimeMillis();

            logger.info(String.format("Insertion used time: %d ms (%d minutes)", insertionEndTime - insertionStartTime, (insertionEndTime - insertionStartTime) / 60000));

            logger.info("Deleting...");

            long deletionStartTime = System.currentTimeMillis();

            for (int i = 0; i < number; i ++)
            {
                int num = randomDeletionList.get(i);
                bPlusTree.delete(num);
                if (i % (number / 2 - 2) == 0)
                {
                    bPlusTree.validate();
//                    logger.info("i: " + i);
//                    bPlusTree.printTree();
                }
            }

            long deletionEndTime = System.currentTimeMillis();

            bPlusTree.printTree();

            logger.info(String.format("Deletion used time: %d ms (%d minutes)", deletionEndTime - deletionStartTime, (deletionEndTime - deletionStartTime) / 60000));
        }
//        catch (IndexOutOfBoundsException | NullPointerException e)
//        {
//            bPlusTree.printTree();
//            throw e;
//        }
        finally
        {
            // waiting for the logger to complete output
            try
            {
                Thread.sleep(20);
            }
            catch (InterruptedException ignored)
            {
            }

            logger.stop();
        }








//        try
//        {
//            long startTime = System.currentTimeMillis();
//
//            List<Integer> inserted = new ArrayList<>();
//            while (inserted.size() < 300000)
//            {
//                int number = (int) (300000 * Math.random()) + 1;
//                if (!inserted.contains(number))
//                {
//                    insert(number, String.valueOf(number));
//                    inserted.add(number);
//                }
//            }
//
//            long insertionEndTime = System.currentTimeMillis();
//
////            logger.info("Inserted: " + Arrays.toString(inserted.toArray()));
////            bPlusTree.printTree();
//
//            // test delete all one by one
//            List<Integer> deleted = new ArrayList<>();
//            while (deleted.size() < 300000)
//            {
//                int number = (int) (300000 * Math.random()) + 1;
//                if (!deleted.contains(number))
//                {
//                    bPlusTree.delete(number);
//                    deleted.add(number);
//                }
//            }
//
//            long endTime = System.currentTimeMillis();
//
////            deleted.sort(Comparator.naturalOrder());
////            logger.info("Deleted: " + Arrays.toString(deleted.toArray()));
//            bPlusTree.printTree();
//
//            logger.info(String.format("Insertion used time: %d ms (%d minutes)", insertionEndTime - startTime, (insertionEndTime - startTime) / 60000));
//            logger.info(String.format("Deletion used time: %d ms (%d minutes)", endTime - insertionEndTime, (endTime - insertionEndTime) / 60000));
//            logger.info(String.format("Total used time: %d ms (%d minutes)", endTime - startTime, (endTime - startTime) / 60000));
//
////            for (int i = 1; i <= 300; i ++)
////            {
////                insert(i, String.valueOf(i));
////            }
////            bPlusTree.printTree();
//        }
//        finally
//        {
//            // waiting for the logger to complete output
//            try
//            {
//                Thread.sleep(20);
//            }
//            catch (InterruptedException ignored)
//            {
//            }
//
//            logger.stop();
//        }
    }

    private static void insert(int key, String value)
    {
        try
        {
            bPlusTree.insert(key, value);
        }
        catch (KeyConflictException e)
        {
            e.printStackTrace();
        }
    }
}
