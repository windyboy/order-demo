package me.windy.demo.order.core.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

/**
 * Tests for Money value object.
 * Verifies invariants and arithmetic operations.
 */
class MoneyTest : DescribeSpec({
    
    describe("Money creation") {
        it("should create Money from string") {
            val money = Money.of("10.50")
            
            money.amount shouldBe BigDecimal("10.50")
        }
        
        it("should create Money from double") {
            val money = Money.of(25.99)
            
            money.amount shouldBe BigDecimal.valueOf(25.99)
        }
        
        it("should create Money from BigDecimal") {
            val value = BigDecimal("100.00")
            val money = Money.of(value)
            
            money.amount shouldBe value
        }
        
        it("should reject negative amounts") {
            shouldThrow<IllegalArgumentException> {
                Money.of("-10.00")
            }
        }
        
        it("should allow zero amount") {
            val money = Money.ZERO
            
            money.amount.compareTo(BigDecimal.ZERO) shouldBe 0
        }
    }
    
    describe("Money arithmetic") {
        it("should add two Money values") {
            val money1 = Money.of("10.50")
            val money2 = Money.of("5.25")
            
            val result = money1 + money2
            
            result shouldBe Money.of("15.75")
        }
        
        it("should multiply Money by integer quantity") {
            val money = Money.of("10.00")
            
            val result = money * 3
            
            result shouldBe Money.of("30.00")
        }
        
        it("should multiply Money by zero") {
            val money = Money.of("10.00")
            
            val result = money * 0
            
            result shouldBe Money.ZERO
        }
        
        it("should reject multiplication by negative quantity") {
            val money = Money.of("10.00")
            
            shouldThrow<IllegalArgumentException> {
                money * -1
            }
        }
        
        it("should handle decimal precision correctly") {
            val money1 = Money.of("10.99")
            val money2 = Money.of("5.01")
            
            val result = money1 + money2
            
            result shouldBe Money.of("16.00")
        }
    }
    
    describe("Money comparison") {
        it("should compare Money values correctly") {
            val money1 = Money.of("10.00")
            val money2 = Money.of("10.00")
            val money3 = Money.of("15.00")
            
            money1 shouldBe money2
            (money1.amount < money3.amount) shouldBe true
        }
    }
})

