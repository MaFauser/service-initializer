package com.mafauser.service

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
    fromApplication<ServiceApplication>().with(TestcontainersConfiguration::class).run(*args)
}
