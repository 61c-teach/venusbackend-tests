/* ktlint-disable package-name */
package venusbackend.linker
/* ktlint-enable package-name */

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import venusbackend.simulator.Simulator
import venusbackend.assembler.Assembler
import venusbackend.assembler.AssemblerError
import venus.vfs.VirtualFileSystem

class LinkerTest {
    @Test
    fun linkOneFile() {
        val (prog, _) = Assembler.assemble("""
        start:
        addi x8 x8 1
        addi x9 x0 2
        beq x8 x9 skip
        jal x0 start
        skip:
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        assertEquals(2, sim.getReg(8))
    }

    @Test fun linkTwoFiles() {
        val (prog1, _) = Assembler.assemble("""
        foo:
            jal x0 bar
            addi x8 x0 8
        .globl foo
        """)
        val (prog2, _) = Assembler.assemble("""
        .globl bar
        bar:
            addi x8 x8 1
        """)
        val PandL = ProgramAndLibraries(listOf(prog1, prog2), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        assertEquals(1, sim.getReg(8))
    }

    @Test fun privateLabel() {
        val (prog1, _) = Assembler.assemble("""
        foo:
            jal x0 _bar
            addi x8 x0 8
        .globl foo
        """)
        val (prog2, _) = Assembler.assemble("""
        _bar:
            addi x8 x8 1
        """)

        try {
            Linker.link(ProgramAndLibraries(listOf(prog1, prog2), VirtualFileSystem("dummy")))
            fail("allowed jump to 'private' label")
        } catch (e: AssemblerError) {
            assertTrue(true)
        }
    }

    @Test fun loadAddress() {
        val (prog, _) = Assembler.assemble("""
        .data
        magic: .byte 42
        .text
        la x8 magic
        lb x9 0(x8)
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        assertEquals(42, sim.getReg(9))
    }

    @Test fun loadAddressBefore() {
        val (prog, _) = Assembler.assemble("""
        .data
        padder:
        .asciiz "padpadpad"
        magic:
        .byte 42
        .text
        nop
        nop
        nop
        nop
        la x8 magic
        lb x9 0(x8)
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        assertEquals(42, sim.getReg(9))
    }

    @Test fun globalLabelTwoFiles() {
        val (prog1, _) = Assembler.assemble("""
        foo:
            addi x8 x0 8
        .globl foo
        """)
        val (prog2, _) = Assembler.assemble("""
        foo:
            addi x8 x0 8
        .globl foo
        """)

        try {
            Linker.link(ProgramAndLibraries(listOf(prog1, prog2), VirtualFileSystem("dummy")))
            fail("allowed global labels in two different files")
        } catch (e: AssemblerError) {
            assertTrue(true)
        }
    }

    @Test fun dataRelocation() {
        val (prog, _) = Assembler.assemble("""
        .data
        A:
        .word 42
        B:
        .word A
        .text
        la x1, A
        lw x2, B
        lw x3, 0(x2)
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        assertEquals(sim.getReg(1), sim.getReg(2))
        assertEquals(42, sim.getReg(3))
    }

    @Test fun dataRelocationAcrossFiles() {
        val (prog1, _) = Assembler.assemble("""
        .data
        .globl A
        .globl B
        A:
        .word 42
        B:
        .word A
        """)
        val (prog2, _) = Assembler.assemble("""
        .text
        la x1, A
        lw x2, B
        lw x3, 0(x2)
        """)
        val PandL = ProgramAndLibraries(listOf(prog1, prog2), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        assertEquals(sim.getReg(1), sim.getReg(2))
        assertEquals(42, sim.getReg(3))
    }

    @Test fun dataRelocationWithMultipleFiles() {
        val (prog1, _) = Assembler.assemble("""
        .data
        world: .string " World!"
        
        .globl main
        .text
        main:
            mv s0 ra
            jal print_char
            la a1 world
            li a0 4 
            ecall
            mv ra s0
            ret
        """)
        val (prog2, _) = Assembler.assemble("""
        .data
        hello: .string "Hello"
        
        .text
        .globl print_char
        print_char:
            la a1 hello
            li a0 4 
            ecall
            ret
        """)
        val PandL = ProgramAndLibraries(listOf(prog1, prog2), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        assertEquals("Hello World!", sim.stdout)
    }

    @Test fun localAndGlobalDataRelocationWithMultipleFiles() {
        val (prog1, _) = Assembler.assemble("""
        .data
        world: .string " World!"
        max: .string "MAX"
        .globl max
        
        .globl main
        .text
        main:
            mv s0 ra
            jal print_char
            la a1 min
            li a0 4 
            ecall
            la a1 world
            li a0 4 
            ecall
            mv ra s0
            ret
        """)
        val (prog2, _) = Assembler.assemble("""
        .data
        hello: .string "Hello "
        min: .string "MIN"
        .globl min
        
        .text
        .globl print_char
        print_char:
            la a1 hello
            li a0 4 
            ecall
            la a1 max
            li a0 4 
            ecall
            ret
        """)
        val PandL = ProgramAndLibraries(listOf(prog1, prog2), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        assertEquals("Hello MAXMIN World!", sim.stdout)
    }
}
