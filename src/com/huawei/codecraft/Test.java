package com.huawei.codecraft;
import java.util.*;
/**
 * @Author: ro_kin
 * @Data:2023/3/14 15:31
 * @Description: TODO
 */

class Person implements Comparable<Person> {
    private String name;
    private int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public int compareTo(Person other) {
        return Integer.compare(other.age,age);
    }

    @Override
    public String toString() {
        return name + " (" + age + ")";
    }
}

class PriorityQueueExample {
    public static void main(String[] args) {
        PriorityQueue<Person> queue = new PriorityQueue<>();

        queue.offer(new Person("Alice", 25));
        queue.offer(new Person("Bob", 20));
        queue.offer(new Person("Charlie", 30));
        queue.offer(new Person("Dave", 22));

        while (!queue.isEmpty()) {
            Person p = queue.poll();
            System.out.println(p);
        }
    }
}
