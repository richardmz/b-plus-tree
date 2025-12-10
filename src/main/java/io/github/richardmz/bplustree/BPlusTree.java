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

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BPlusTree<K extends Comparable<? super K>, V>
{
    private final Logger logger = Logger.getInstance();
    private final int degree;

    private final int minKeyArraySize;

    private final LeafNode<K, V> firstLeaf;
    private Node<K> root;

    public BPlusTree(int degree) throws DegreeTooSmallException
    {
        if (degree < 3)
        {
            throw new DegreeTooSmallException(degree);
        }
        else
        {
            this.degree = degree;
            this.minKeyArraySize = (degree % 2 == 0) ? (degree / 2 - 1) : ((degree + 1) / 2 - 1);
            this.root = new LeafNode<>();
            this.firstLeaf = (LeafNode<K, V>) root;
        }
    }


    // Insertion =======================================================================================================

    public void insert(K key, V value) throws KeyConflictException
    {
//        logger.debug("Inserting " + key);
        Node<K> newChild = insert(root, key, value);
        if (newChild != null)
        {
            // split root node, create new root node
            InternalNode<K> newRoot = new InternalNode<>();
            newRoot.keys.add(newChild.getMinKey());
            newRoot.children.add(root);
            newRoot.children.add(newChild);
            root = newRoot;
        }
    }

    private Node<K> insert(Node<K> node, K key, V value) throws KeyConflictException
    {
        if (node.isLeaf())
        {
            int pos = Collections.binarySearch(node.keys, key);
            // found
            if (pos >= 0)
            {
                throw new KeyConflictException(key.toString());
            }
            // not found
            else
            {
                LeafNode<K, V> leaf = (LeafNode<K, V>) node;
                int insertPos = -(pos + 1); // See doc of Collections.binarySearch(list, key)
                leaf.keys.add(insertPos, key);
                leaf.values.add(insertPos, value);
                // need split
                if (leaf.keys.size() == degree)
                {
                    return split(leaf);
                }
                else
                {
                    return null;
                }
            }
        }
        else // is internal
        {
            int pos = Collections.binarySearch(node.keys, key);
            // found
            if (pos >= 0)
            {
                throw new KeyConflictException(key.toString());
            }
            // not found
            else
            {
                InternalNode<K> internalNode = (InternalNode<K>) node;
                int insertPos = -(pos + 1); // See doc of Collections.binarySearch(list, key)
//                logger.debug("Keys: " + internalNode.keys);
//                debugPrintChildrenKeys(internalNode);
//                logger.debug("insertPos: " + insertPos);
//                logger.debugBreakLine();
                Node<K> child = internalNode.children.get(insertPos);
                Node<K> newChild = insert(child, key, value);
                if (newChild != null)
                {
                    internalNode.keys.add(insertPos, newChild.getMinKey());
                    internalNode.children.add(insertPos + 1, newChild);
                    if (internalNode.keys.size() == degree)
                    {
                        return split(internalNode);
                    }
                    else
                    {
                        return null;
                    }
                }
                else
                {
                    return null;
                }
            }
        }
    }

    private LeafNode<K, V> split(LeafNode<K, V> leaf)
    {
//        logger.debug("Splitting leaf, keys: " + leaf.keys);
        int medianPos = degree / 2;
        LeafNode<K, V> newLeaf = new LeafNode<>();
        newLeaf.keys.addAll(leaf.keys.subList(medianPos, leaf.keys.size()));
        newLeaf.values.addAll(leaf.values.subList(medianPos, leaf.values.size()));
        leaf.keys.subList(medianPos, leaf.keys.size()).clear();
        leaf.values.subList(medianPos, leaf.values.size()).clear();
        newLeaf.next = leaf.next;
        leaf.next = newLeaf;
        return newLeaf;
    }

    private InternalNode<K> split(InternalNode<K> internal)
    {
//        logger.debug("Splitting internal node, keys: " + internal.keys);
        int medianPos = degree / 2 + 1;
        InternalNode<K> newInternal = new InternalNode<>();
        newInternal.keys.addAll(internal.keys.subList(medianPos, internal.keys.size()));
        newInternal.children.addAll(internal.children.subList(medianPos, internal.children.size()));
        internal.keys.subList(medianPos - 1, internal.keys.size()).clear();
        internal.children.subList(medianPos, internal.children.size()).clear();
        return newInternal;
    }

    private void debugPrintChildrenKeys(InternalNode<K> node)
    {
        logger.debug("Children keys: ", false);
        for (Node<K> child : node.children)
        {
            logger.debugInline(child.keys + " ");
        }
        logger.debugInline("\n");
    }


    // Search Methods ==================================================================================================

    @Nullable
    public V search(K key)
    {
//        logger.debug("Searching: " + key);
        return search(root, key);
    }

    @Nullable
    private V search(Node<K> node, K key)
    {
        if (node.isLeaf())
        {
            LeafNode<K, V> leaf = (LeafNode<K, V>) node;
//            logger.debug("Leaf keys: " + leaf.keys);
            int pos = Collections.binarySearch(leaf.keys, key);
//            logger.debug("Position: " + pos);
            // found
            if (pos >= 0)
            {
                return leaf.values.get(pos);
            }
            else
            {
                return null;
            }
        }
        else // is internal
        {
            InternalNode<K> internalNode = (InternalNode<K>) node;
//            logger.debug("Internal keys: " + node.keys);
            int keyPos = Collections.binarySearch(internalNode.keys, key);
            int childPos;
            // found
            if (keyPos >= 0)
            {
                childPos = keyPos + 1; // when is found in internal, childPos = keyPos + 1
            }
            else
            {
                childPos = -(keyPos + 1);
            }
//            logger.debug("Child position: " + childPos);
            return search(internalNode.children.get(childPos), key);
        }
    }

    public List<V> rangeQuery(K lowerKey, K upperKey)
    {
//        logger.debug("Querying from: " + lowerKey + " to " + upperKey);
        LeafNode<K, V> leaf = firstLeaf;
        List<V> result = new ArrayList<>();
        rangeQueryInLeaf(leaf, lowerKey, upperKey, result);
        while (leaf.hasNext())
        {
            leaf = leaf.next;
            rangeQueryInLeaf(leaf, lowerKey, upperKey, result);
        }
        return result;
    }

    private void rangeQueryInLeaf(LeafNode<K,V> leaf, K lowerKey, K upperKey, List<V> result)
    {
        for (int pos = 0; pos < leaf.keys.size(); pos ++)
        {
            K key = leaf.keys.get(pos);
            if (key.compareTo(lowerKey) >= 0 && key.compareTo(upperKey) <= 0)
            {
                result.add(leaf.values.get(pos));
            }
        }
    }


    // Deletion ========================================================================================================

    public void delete(K key)
    {
//        logger.debug("Deleting: " + key);
        delete(root, key);
    }

    private boolean delete(Node<K> node, K key)
    {
        if (node.isLeaf())
        {
            LeafNode<K, V> leaf = (LeafNode<K, V>) node;
//            logger.debug("Leaf keys: " + leaf.keys);
            int keyPos = Collections.binarySearch(leaf.keys, key);
//            logger.debug("Position: " + keyPos);
            // found
            if (keyPos >= 0)
            {
                leaf.keys.remove(keyPos);
                leaf.values.remove(keyPos);
                return true;
            }
            else
            {
                return false;
            }
        }
        else // is internal
        {
            InternalNode<K> internalNode = (InternalNode<K>) node;
//            logger.debug("Internal keys: " + node.keys);
            int keyPos = Collections.binarySearch(node.keys, key);
            // found
            if (keyPos >= 0)
            {
//                logger.debug("Found in internal");
                int childPos = keyPos + 1;
//                logger.debug("Key Position: " + keyPos);
//                logger.debug("Child Position: " + childPos);
                Node<K> child = internalNode.children.get(childPos);
                delete(child, key);
                // update the key with successor
                if (child.isLeaf() && child.keys.size() == 0 && hasRightSibling(internalNode, childPos))
                {
//                    logger.info("child.getMinKey() == null, deleting: " + key);
                    Node<K> right = getRightSibling(internalNode, childPos);
                    internalNode.keys.set(keyPos, right.getMinKey());
                    internalNode.keys.remove(keyPos + 1);
                    internalNode.children.set(childPos, right);
                    internalNode.children.remove(childPos + 1);
                }
                else
                {
                    internalNode.keys.set(keyPos, child.getMinKey());
//                    logger.debug("internalNode.keys: " + internalNode.keys);
//                    logger.debug("internalNode.keys.size(): " + internalNode.keys.size());
//                    logger.debug("child.keys: " + child.keys);
                    // need to adjust nodes
                    if (child.keys.size() < minKeyArraySize)
                    {
                        adjustNodes(internalNode, true, child, keyPos, childPos);
//                        printTree();
                    }
                }
                return true;
            }
            // not found, try to delete from the child at the insertion point
            else
            {
                int childPos = -(keyPos + 1);
                keyPos = Math.max(childPos - 1, 0);
//                logger.debug("Key Position: " + keyPos);
//                logger.debug("Child Position: " + childPos);
                Node<K> child = internalNode.children.get(childPos);
                boolean success = delete(child, key);
                if (success)
                {
//                    logger.debug("child.keys: " + child.keys);
                    // need to adjust nodes
                    if (child.keys.size() < minKeyArraySize)
                    {
                        adjustNodes(internalNode, false, child, keyPos, childPos);
//                        printTree();
                    }
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }
    }

    private void adjustNodes(InternalNode<K> parent, boolean found, Node<K> child, int keyPos, int childPos)
    {
        if (hasLeftSibling(childPos) && leftSiblingHasExtraKeys(parent, childPos))
        {
            borrowFromLeft(parent, child, keyPos, childPos);
            // update the key
            parent.keys.set(keyPos, child.getMinKey());
        }
        else if (hasRightSibling(parent, childPos) && rightSiblingHasExtraKeys(parent, childPos))
        {
            borrowFromRight(parent, child, keyPos, childPos);
        }
        else if (hasLeftSibling(childPos))
        {
            Node<K> left = mergeWithLeft(parent, child, keyPos, childPos);
            parent.keys.remove(keyPos);
            parent.children.remove(childPos);
            if (parent == root && parent.keys.size() == 0)
            {
                root = left;
            }
        }
        else if (hasRightSibling(parent, childPos))
        {
            mergeWithRight(parent, child, keyPos, childPos);
            parent.keys.remove(keyPos);
            parent.children.remove(childPos + 1);
            if (parent == root && parent.keys.size() == 0)
            {
                root = child;
            }
        }
        else
        {
            throw new IllegalStateException("Fatal Error - Should not reach here");
        }
    }

    private void borrowFromLeft(InternalNode<K> parent, Node<K> child, int keyPos, int childPos)
    {
//        logger.debug("Borrowing from left");
        Node<K> left = getLeftSibling(parent, childPos);
        if (child.isLeaf())
        {
            LeafNode<K, V> childLeaf = (LeafNode<K, V>) child;
            LeafNode<K, V> leftLeaf = (LeafNode<K, V>) left;
            childLeaf.keys.add(0, leftLeaf.keys.get(left.keys.size() - 1));
            leftLeaf.keys.remove(leftLeaf.keys.size() - 1);
            childLeaf.values.add(0, leftLeaf.values.get(leftLeaf.values.size() - 1));
            leftLeaf.values.remove(leftLeaf.values.size() - 1);
        }
        else // child is internal
        {
            InternalNode<K> childInternal = (InternalNode<K>) child;
            InternalNode<K> leftInternal = (InternalNode<K>) left;
            childInternal.keys.add(0, parent.keys.get(keyPos));
            parent.keys.set(keyPos, leftInternal.keys.get(left.keys.size() - 1));
            leftInternal.keys.remove(leftInternal.keys.size() - 1);
            childInternal.children.add(0, leftInternal.children.get(leftInternal.children.size() - 1));
            leftInternal.children.remove(leftInternal.children.size() - 1);
        }
    }

    private void borrowFromRight(InternalNode<K> parent, Node<K> child, int keyPos, int childPos)
    {
//        logger.debug("Borrowing from right");
        Node<K> right = getRightSibling(parent, childPos);
//        logger.debug("right: " + right.keys);
        if (child.isLeaf())
        {
            LeafNode<K, V> childLeaf = (LeafNode<K, V>) child;
            LeafNode<K, V> rightLeaf = (LeafNode<K, V>) right;
            K borrowed = rightLeaf.keys.get(0);
            childLeaf.keys.add(borrowed);
            rightLeaf.keys.remove(0);
            int compareResult = borrowed.compareTo(parent.keys.get(keyPos));
            // first child borrow key from second child
            if (compareResult == 0)
            {
                // update the key
                parent.keys.set(keyPos, rightLeaf.getMinKey());
            }
            else
            {
                // update the next key
                parent.keys.set(keyPos + 1, rightLeaf.getMinKey());
            }
            childLeaf.values.add(rightLeaf.values.get(0));
            rightLeaf.values.remove(0);
        }
        else // child is internal
        {
            InternalNode<K> childInternal = (InternalNode<K>) child;
            InternalNode<K> rightInternal = (InternalNode<K>) right;
            K borrowed = rightInternal.keys.get(0);
            if (parent.keys.size() == 1)
            {
                childInternal.keys.add(parent.keys.get(keyPos));
                parent.keys.set(keyPos, borrowed);
            }
            else
            {
                int compareResult = borrowed.compareTo(parent.keys.get(keyPos + 1));
                if (compareResult > 0)
                {
                    childInternal.keys.add(parent.keys.get(keyPos + 1));
                    parent.keys.set(keyPos + 1, borrowed);
                }
                else if (compareResult == 0)
                {
                    throw new IllegalStateException("Fatal Error - Should not reach here");
                }
                else
                {
                    childInternal.keys.add(parent.keys.get(keyPos));
                    parent.keys.set(keyPos, borrowed);
                }
            }
            rightInternal.keys.remove(0);
            childInternal.children.add(rightInternal.children.get(0));
            rightInternal.children.remove(0);
        }
    }

    private Node<K> mergeWithLeft(InternalNode<K> parent, Node<K> child, int keyPos, int childPos)
    {
        Node<K> left = getLeftSibling(parent, childPos);
//        logger.debug(String.format("Merging child %s with left %s", child.keys, left.keys));
        if (child.isLeaf())
        {
            LeafNode<K, V> childLeaf = (LeafNode<K, V>) child;
            LeafNode<K, V> leftLeaf = (LeafNode<K, V>) left;
            leftLeaf.keys.addAll(childLeaf.keys);
            leftLeaf.values.addAll(childLeaf.values);
            leftLeaf.next = childLeaf.next; // remember to update the pointer
        }
        else // child is internal
        {
            InternalNode<K> childInternal = (InternalNode<K>) child;
            InternalNode<K> leftInternal = (InternalNode<K>) left;
            leftInternal.keys.add(parent.keys.get(keyPos));
            leftInternal.keys.addAll(childInternal.keys);
            leftInternal.children.addAll(childInternal.children);
        }
        return left;
    }

    private void mergeWithRight(InternalNode<K> parent, Node<K> child, int keyPos, int childPos)
    {
//        logger.debug("Merging with right");
        Node<K> right = getRightSibling(parent, childPos);
        if (child.isLeaf())
        {
            LeafNode<K, V> childLeaf = (LeafNode<K, V>) child;
            LeafNode<K, V> rightLeaf = (LeafNode<K, V>) right;
            childLeaf.keys.addAll(rightLeaf.keys);
            childLeaf.values.addAll(rightLeaf.values);
            childLeaf.next = rightLeaf.next; // remember to update the pointer
        }
        else // child is internal
        {
            InternalNode<K> childInternal = (InternalNode<K>) child;
            InternalNode<K> rightInternal = (InternalNode<K>) right;
            childInternal.keys.add(parent.keys.get(keyPos));
            childInternal.keys.addAll(rightInternal.keys);
            childInternal.children.addAll(rightInternal.children);
        }
    }

    private boolean leftSiblingHasExtraKeys(InternalNode<K> parent, int pos)
    {
        return hasExtraKeys(getLeftSibling(parent, pos));
    }

    private boolean rightSiblingHasExtraKeys(InternalNode<K> parent, int pos)
    {
        return hasExtraKeys(getRightSibling(parent, pos));
    }

    private boolean hasLeftSibling(int pos)
    {
        return pos > 0;
    }

    private boolean hasRightSibling(InternalNode<K> parent, int pos)
    {
        return parent.children.size() - 1 > pos;
    }

    private Node<K> getLeftSibling(InternalNode<K> parent, int pos)
    {
        return parent.children.get(pos - 1);
    }

    private Node<K> getRightSibling(InternalNode<K> parent, int pos)
    {
        return parent.children.get(pos + 1);
    }

    private boolean hasExtraKeys(Node<K> node)
    {
        return node.keys.size() > minKeyArraySize;
    }


    // Validation ======================================================================================================

    public boolean validate()
    {
        logger.info("Validating ...");
        if (validate(root))
        {
            if (validateLeafs())
            {
                logger.info("Validation passed");
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public boolean validate(Node<K> node)
    {
        if (node == null)
        {
            logger.info("Validation failed: null pointer found");
            return false;
        }
        else
        {
            if (node.isLeaf())
            {
//                logger.info("node " + node.keys + " is leaf");
                LeafNode<K, V> leaf = (LeafNode<K, V>) node;
                if (leaf.keys.size() != leaf.values.size())
                {
                    logger.info("Validation failed: leaf.keys.size() != leaf.values.size()");
                    return false;
                }
                else
                {
                    if (leaf.keys.size() >= degree)
                    {
                        logger.info("Validation failed: leaf.keys.size() >= degree");
                        return false;
                    }
                    else if (leaf != root && leaf.keys.size() < Math.ceil((double) degree / 2) - 1)
                    {
                        logger.info("Validation failed: leaf.keys.size() < Math.ceil((double) degree / 2) - 1");
                        return false;
                    }
                    else
                    {
                        return validateKeysOrder(leaf);
                    }
                }
            }
            else
            {
//                logger.info("node " + node.keys + " is internal");
                InternalNode<K> internal = (InternalNode<K>) node;
                if (internal.keys.size() + 1 != internal.children.size())
                {
                    logger.info("Validation failed: internal.keys.size() + 1 != internal.children.size()");
                    return false;
                }
                else
                {
                    if (internal.keys.size() >= degree)
                    {
                        logger.info("Validation failed: internal.keys.size() >= degree");
                        return false;
                    }
                    else if (internal != root && (internal.keys.size() < Math.ceil((double) degree / 2) - 1))
                    {
                        logger.info("Validation failed: internal.keys.size() < Math.ceil((double) degree / 2) - 1");
                        logger.info("Detail: " + internal.keys);
                        return false;
                    }
                    else
                    {
                        if (validateKeysOrder(internal))
                        {
                            return validateChildren(internal);
                        }
                        else
                        {
                            return false;
                        }
                    }
                }
            }
        }
    }

    private boolean validateChildren(InternalNode<K> internal)
    {
        for (int i = 0; i < internal.children.size(); i ++)
        {
            Node<K> child = internal.children.get(i);
            if (i == internal.children.size() - 1)
            {
                if (child.getMinKey().compareTo(internal.keys.get(i - 1)) < 0)
                {
                    logger.info("Validation failed: child.getMinKey().compareTo(internal.keys.get(i - 1)) < 0");
                    return false;
                }
                else
                {
                    if (!validate(child))
                    {
                        return validate(child);
                    }
                    else
                    {
                        // continue
                    }
                }
            }
            else
            {
                if (child.getMinKey().compareTo(internal.keys.get(i)) >= 0)
                {
                    logger.info("Validation failed: child.getMinKey().compareTo(internal.keys.get(i)) >= 0");
                    return false;
                }
                else
                {
                    if (!validate(child))
                    {
                        return false;
                    }
                    else
                    {
                        // continue
                    }
                }
            }
        }
        return true;
    }

    private boolean validateKeysOrder(Node<K> node)
    {
        for (int i = 0; i < node.keys.size() - 1; i ++)
        {
            if (node != root && node.keys.get(i).compareTo(node.keys.get(i + 1)) >= 0)
            {
                logger.info("Validation failed: node.keys.get(i).compareTo(node.keys.get(i + 1)) >= 0");
                return false;
            }
        }
        return true;
    }


    private boolean validateLeafs()
    {
        LeafNode<K, V> leaf = firstLeaf;
        List<K> keys = null;
        while (leaf != null)
        {
            if (keys != null)
            {
                K leftKey = keys.get(keys.size() - 1);
                K rightKey = leaf.keys.get(leaf.keys.size() - 1);
                if (leftKey.compareTo(rightKey) >= 0)
                {
                    return false;
                }
            }
            keys = leaf.keys;
            leaf = leaf.next;
        }
        return true;
    }


    // Visualization ===================================================================================================

    public void printTree()
    {
        logger.info("Tree Structure: ");
        Queue<Node<K>> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty())
        {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++)
            {
                Node<K> node = queue.poll();
                assert node != null;
                logger.infoInline(node.keys + " ");

                if (!node.isLeaf())
                {
                    InternalNode<K> internal = (InternalNode<K>) node;
                    queue.addAll(internal.children);
                }
            }
            logger.infoBreakLine();
        }
    }

    public void printLeafs()
    {
        logger.info("Leafs: ");
        LeafNode<K, V> node = firstLeaf;
        logger.info(node.keys.toString());
        while (node.hasNext())
        {
            node = node.next;
            logger.info(node.keys.toString());
        }
    }

    public void printLeafValues()
    {
        logger.info("Values: ");
        LeafNode<K, V> leaf = firstLeaf;
        logger.info(leaf.values.toString());
        while (leaf.hasNext())
        {
            leaf = leaf.next;
            logger.info(leaf.values.toString());
        }
    }

    public void printKeys(Node<K> node)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        StringJoiner stringJoiner = new StringJoiner(", ");
        for (K key : node.keys)
        {
            stringJoiner.add(key.toString());
        }
        stringBuilder.append(stringJoiner);
        stringBuilder.append("]");
        logger.info(stringBuilder.toString());
    }
}
