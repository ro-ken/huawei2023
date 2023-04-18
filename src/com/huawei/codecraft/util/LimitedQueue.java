package com.huawei.codecraft.util;

/**
 * @author :ro_kin
 * @date : 2023/4/16
 */
import com.huawei.codecraft.Main;
import java.util.LinkedList;

public class LimitedQueue<E> extends LinkedList<E> {
    private int limit;
    private Node first;
    public Node last;

    public class Node {
        E value;
        Node next;
        Node prev;

        Node(E value, Node next, Node prev) {
            this.value = value;
            this.next = next;
            this.prev = prev;
        }
    }

    public LimitedQueue(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add(E o) {
        Node newNode = new Node(o, null, last);
        if (last != null) {
            last.next = newNode;
        } else {
            first = newNode;
        }
        last = newNode;
        super.add(o);
        while (size() > limit) {
            super.remove();
            first = first.next;
            if (first != null) {
                first.prev = null;
            }
        }
        return true;
    }

//    public void forwardPrint() {
//        for (E e : this) {
//            System.out.print(e + " ");
//        }
//        System.out.println();
//    }
//
//    public void reversePrint() {
//        Node currentNode = last;
//        while (currentNode != null) {
//            System.out.print(currentNode.value + " ");
//            currentNode = currentNode.prev;
//        }
//        System.out.println();
//    }
}