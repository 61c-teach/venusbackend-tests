package venusbackend.simulator

import kotlin.test.Test
import venusbackend.assembler.Assembler
import venus.vfs.VirtualFileSystem
import venusbackend.linker.Linker
import venusbackend.linker.ProgramAndLibraries
import venusbackend.simulator.Tracer.Companion.wordAddressed
import kotlin.test.assertEquals

class TracerTest {

    fun trace(tr: Tracer): String {
        tr.traceStart()
        while (!tr.sim.isDone()) {
            tr.traceStep()
        }
        tr.traceEnd()
        tr.traceStringStart()
        while (tr.traceStringStep()) {}
        tr.traceStringEnd()
        return tr.getString()
    }

    fun makeSim(text: String, simSettings: SimulatorSettings = SimulatorSettings()): Simulator {
        val (prog, _) = Assembler.assemble(text)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        return Simulator(linked, settings = simSettings)
    }

    @Test
    fun addiJalrInstFirstTwoStageTest() {
        val ss = SimulatorSettings()
        ss.setRegesOnInit = false
        val sim = makeSim("""
        main:
            addi t0 x0 1
            addi t1 x0 16
            jalr t0 t1 0
        func1:
            addi s0 x0 0x01
        func2:
            addi s1 x0 0x02
        """, ss)
        val t = Tracer(sim)
        t.format = "ra:  %x1% sp:  %x2% t0:  %5% t1:  %6% t2:  %7% s0:  %8% s1:  %9% a0:  %10% PC:  %pc% inst:  %inst% Time_Step:  %line%\\n"
        t.instFirst = true
        wordAddressed = true
        t.base = 16
        t.twoStage = true
        val traceString = trace(t)
        assertEquals("""ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000000 inst:  00100293 Time_Step:  0000
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000001 inst:  01000313 Time_Step:  0001
ra:  00000000 sp:  00000000 t0:  00000001 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000002 inst:  000302e7 Time_Step:  0002
ra:  00000000 sp:  00000000 t0:  00000001 t1:  00000010 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000003 inst:  00100413 Time_Step:  0003
ra:  00000000 sp:  00000000 t0:  0000000c t1:  00000010 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000004 inst:  00200493 Time_Step:  0004
ra:  00000000 sp:  00000000 t0:  0000000c t1:  00000010 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000005 inst:  00000000 Time_Step:  0005
ra:  00000000 sp:  00000000 t0:  0000000c t1:  00000010 t2:  00000000 s0:  00000000 s1:  00000002 a0:  00000000 PC:  00000006 inst:  00000000 Time_Step:  0006
""", traceString)
    }

    @Test
    fun addluisllInstFirstTwoStageTest() {
        val ss = SimulatorSettings()
        ss.setRegesOnInit = false
        val sim = makeSim("""
        addi s0 s0 1
        addi s1 x0 14
        sll s0 s0 s1
        add s1 s0 x0
        add t0 s1 s0
        lui ra 0xfffff
        """, ss)
        val t = Tracer(sim)
        t.format = "ra:  %x1% sp:  %x2% t0:  %5% t1:  %6% t2:  %7% s0:  %8% s1:  %9% a0:  %10% PC:  %pc% inst:  %inst% Time_Step:  %line%\\n"
        t.instFirst = true
        wordAddressed = true
        t.base = 16
        t.twoStage = true
        val traceString = trace(t)
        assertEquals("""ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000000 inst:  00140413 Time_Step:  0000
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000001 inst:  00e00493 Time_Step:  0001
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000001 s1:  00000000 a0:  00000000 PC:  00000002 inst:  00941433 Time_Step:  0002
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000001 s1:  0000000e a0:  00000000 PC:  00000003 inst:  000404b3 Time_Step:  0003
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00004000 s1:  0000000e a0:  00000000 PC:  00000004 inst:  008482b3 Time_Step:  0004
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00004000 s1:  00004000 a0:  00000000 PC:  00000005 inst:  fffff0b7 Time_Step:  0005
ra:  00000000 sp:  00000000 t0:  00008000 t1:  00000000 t2:  00000000 s0:  00004000 s1:  00004000 a0:  00000000 PC:  00000006 inst:  00000000 Time_Step:  0006
ra:  fffff000 sp:  00000000 t0:  00008000 t1:  00000000 t2:  00000000 s0:  00004000 s1:  00004000 a0:  00000000 PC:  00000007 inst:  00000000 Time_Step:  0007
""", traceString)
    }

    @Test
    fun addiInstFirstTwoStageTest() {
        val ss = SimulatorSettings()
        ss.setRegesOnInit = false
        val sim = makeSim("""
        addi t0, x0, 5
        addi t1, t0, 7
        addi s0, t0, 9
        """, ss)
        val t = Tracer(sim)
        t.format = "ra:  %x1% sp:  %x2% t0:  %5% t1:  %6% t2:  %7% s0:  %8% s1:  %9% a0:  %10% PC:  %pc% inst:  %inst% Time_Step:  %line%\\n"
        t.instFirst = true
        wordAddressed = true
        t.base = 16
        t.twoStage = true
        t.totCommands = 6
        val traceString = trace(t)
        assertEquals("""ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000000 inst:  00500293 Time_Step:  0000
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000001 inst:  00728313 Time_Step:  0001
ra:  00000000 sp:  00000000 t0:  00000005 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000002 inst:  00928413 Time_Step:  0002
ra:  00000000 sp:  00000000 t0:  00000005 t1:  0000000c t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000003 inst:  00000000 Time_Step:  0003
ra:  00000000 sp:  00000000 t0:  00000005 t1:  0000000c t2:  00000000 s0:  0000000e s1:  00000000 a0:  00000000 PC:  00000004 inst:  00000000 Time_Step:  0004
ra:  00000000 sp:  00000000 t0:  00000005 t1:  0000000c t2:  00000000 s0:  0000000e s1:  00000000 a0:  00000000 PC:  00000005 inst:  00000000 Time_Step:  0005
""", traceString)
    }

    @Test
    fun brjalrInstFirstTwoStageTest() {
        val ss = SimulatorSettings()
        ss.setRegesOnInit = false
        val sim = makeSim("""
        add s0 x0 x0
        addi a0 x0 -1
        bne s0 s0 never_reach
        addi s0 s0 -1
        lui s1 0 #end
        addi s1 s1 36 #end
        jr s1
        never_reach:
          addi s0, s0, 1
          j end
        end:
          addi a0 a0 1

        """, ss)
        val t = Tracer(sim)
        t.format = "ra:  %x1% sp:  %x2% t0:  %5% t1:  %6% t2:  %7% s0:  %8% s1:  %9% a0:  %10% PC:  %pc% inst:  %inst% Time_Step:  %line%\\n"
        t.instFirst = true
        wordAddressed = true
        t.base = 16
        t.twoStage = true
        val traceString = trace(t)
        assertEquals("""ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000000 inst:  00000433 Time_Step:  0000
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000001 inst:  fff00513 Time_Step:  0001
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000002 inst:  00841a63 Time_Step:  0002
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  ffffffff PC:  00000003 inst:  fff40413 Time_Step:  0003
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  ffffffff PC:  00000004 inst:  000004b7 Time_Step:  0004
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  ffffffff s1:  00000000 a0:  ffffffff PC:  00000005 inst:  02448493 Time_Step:  0005
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  ffffffff s1:  00000000 a0:  ffffffff PC:  00000006 inst:  00048067 Time_Step:  0006
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  ffffffff s1:  00000024 a0:  ffffffff PC:  00000007 inst:  00140413 Time_Step:  0007
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  ffffffff s1:  00000024 a0:  ffffffff PC:  00000009 inst:  00150513 Time_Step:  0008
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  ffffffff s1:  00000024 a0:  ffffffff PC:  0000000a inst:  00000000 Time_Step:  0009
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  ffffffff s1:  00000024 a0:  00000000 PC:  0000000b inst:  00000000 Time_Step:  000a
""", traceString)
    }

    @Test
    fun branchesInstFirstTwoStageTest() {
        val ss = SimulatorSettings()
        ss.setRegesOnInit = false
        val sim = makeSim("""
            beq s0 s0 start #0
        bad-loop:
            addi sp sp -1 #1
            beq x0 x0 bad-loop #2

        start:
            addi s1 s1 10#3
            blt s0 s1 label1#4
            beq x0 x0 bad-loop#5

        label2:
            addi s1 s1 -20#6
            bltu s0 s1 end#7
            beq x0 x0 bad-loop#8

        label1:
            addi s0 s0 20#9
            blt s1 s0 label2#10
            beq x0 x0 bad-loop#11

        end:
            add a0 x0 x0#12

        #0,3,4,9,10,6,7,12
        """, ss)
        val t = Tracer(sim)
        t.format = "ra:  %x1% sp:  %x2% t0:  %5% t1:  %6% t2:  %7% s0:  %8% s1:  %9% a0:  %10% PC:  %pc% inst:  %inst% Time_Step:  %line%\\n"
        t.instFirst = true
        wordAddressed = true
        t.base = 16
        t.twoStage = true
        val traceString = trace(t)
        assertEquals("""ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000000 inst:  00840663 Time_Step:  0000
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000001 inst:  fff10113 Time_Step:  0001
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000003 inst:  00a48493 Time_Step:  0002
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000004 inst:  00944a63 Time_Step:  0003
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  0000000a a0:  00000000 PC:  00000005 inst:  fe0008e3 Time_Step:  0004
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  0000000a a0:  00000000 PC:  00000009 inst:  01440413 Time_Step:  0005
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  0000000a a0:  00000000 PC:  0000000a inst:  fe84c8e3 Time_Step:  0006
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000014 s1:  0000000a a0:  00000000 PC:  0000000b inst:  fc000ce3 Time_Step:  0007
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000014 s1:  0000000a a0:  00000000 PC:  00000006 inst:  fec48493 Time_Step:  0008
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000014 s1:  0000000a a0:  00000000 PC:  00000007 inst:  00946a63 Time_Step:  0009
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000014 s1:  fffffff6 a0:  00000000 PC:  00000008 inst:  fe0002e3 Time_Step:  000a
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000014 s1:  fffffff6 a0:  00000000 PC:  0000000c inst:  00000533 Time_Step:  000b
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000014 s1:  fffffff6 a0:  00000000 PC:  0000000d inst:  00000000 Time_Step:  000c
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000014 s1:  fffffff6 a0:  00000000 PC:  0000000e inst:  00000000 Time_Step:  000d
""", traceString)
    }

    @Test
    fun jumpInstFirstTwoStageTest() {
        val ss = SimulatorSettings()
        ss.setRegesOnInit = false
        val sim = makeSim("""
        jal ra label
        addi s0 x0 -1
        jal x0 end
        label: jalr x0 ra 0
        end: addi a0 x0 -1

        #0,3,1,2,4
        """, ss)
        val t = Tracer(sim)
        t.format = "ra:  %x1% sp:  %x2% t0:  %5% t1:  %6% t2:  %7% s0:  %8% s1:  %9% a0:  %10% PC:  %pc% inst:  %inst% Time_Step:  %line%\\n"
        t.instFirst = true
        wordAddressed = true
        t.base = 16
        t.twoStage = true
        val traceString = trace(t)
        assertEquals("""ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000000 inst:  00c000ef Time_Step:  0000
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000001 inst:  fff00413 Time_Step:  0001
ra:  00000004 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000003 inst:  00008067 Time_Step:  0002
ra:  00000004 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000004 inst:  fff00513 Time_Step:  0003
ra:  00000004 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000001 inst:  fff00413 Time_Step:  0004
ra:  00000004 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000002 inst:  0080006f Time_Step:  0005
ra:  00000004 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  ffffffff s1:  00000000 a0:  00000000 PC:  00000003 inst:  00008067 Time_Step:  0006
ra:  00000004 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  ffffffff s1:  00000000 a0:  00000000 PC:  00000004 inst:  fff00513 Time_Step:  0007
ra:  00000004 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  ffffffff s1:  00000000 a0:  00000000 PC:  00000005 inst:  00000000 Time_Step:  0008
ra:  00000004 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  ffffffff s1:  00000000 a0:  ffffffff PC:  00000006 inst:  00000000 Time_Step:  0009
""", traceString)
    }

    @Test
    fun memInstFirstTwoStageTest() {
        val ss = SimulatorSettings()
        ss.setRegesOnInit = false
        val sim = makeSim("""
        lui s0 74565
        addi s0 s0 1656
        sw s0 40(x0)
        lw ra 40(x0)
        """, ss)
        val t = Tracer(sim)
        t.format = "ra:  %x1% sp:  %x2% t0:  %5% t1:  %6% t2:  %7% s0:  %8% s1:  %9% a0:  %10% PC:  %pc% inst:  %inst% Time_Step:  %line%\\n"
        t.instFirst = true
        wordAddressed = true
        t.base = 16
        t.twoStage = true
        val traceString = trace(t)
        assertEquals("""ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000000 inst:  12345437 Time_Step:  0000
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000001 inst:  67840413 Time_Step:  0001
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  12345000 s1:  00000000 a0:  00000000 PC:  00000002 inst:  02802423 Time_Step:  0002
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  12345678 s1:  00000000 a0:  00000000 PC:  00000003 inst:  02802083 Time_Step:  0003
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  12345678 s1:  00000000 a0:  00000000 PC:  00000004 inst:  00000000 Time_Step:  0004
ra:  12345678 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  12345678 s1:  00000000 a0:  00000000 PC:  00000005 inst:  00000000 Time_Step:  0005
""", traceString)
    }

    @Test
    fun moreBranchesInstFirstTwoStageTest() {
        val ss = SimulatorSettings()
        ss.setRegesOnInit = false
        val sim = makeSim("""
        addi t0 x0 3
        addi t1 x0 3
        blt t0 t1 label
        addi t2 t2 11
        blt t1 t2 label2
        label:
          addi t2 t2 1
        label2:
          addi t1 t1 2
        """, ss)
        val t = Tracer(sim)
        t.format = "ra:  %x1% sp:  %x2% t0:  %5% t1:  %6% t2:  %7% s0:  %8% s1:  %9% a0:  %10% PC:  %pc% inst:  %inst% Time_Step:  %line%\\n"
        t.instFirst = true
        wordAddressed = true
        t.base = 16
        t.twoStage = true
        val traceString = trace(t)
        assertEquals("""ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000000 inst:  00300293 Time_Step:  0000
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000001 inst:  00300313 Time_Step:  0001
ra:  00000000 sp:  00000000 t0:  00000003 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000002 inst:  0062c663 Time_Step:  0002
ra:  00000000 sp:  00000000 t0:  00000003 t1:  00000003 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000003 inst:  00b38393 Time_Step:  0003
ra:  00000000 sp:  00000000 t0:  00000003 t1:  00000003 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000004 inst:  00734463 Time_Step:  0004
ra:  00000000 sp:  00000000 t0:  00000003 t1:  00000003 t2:  0000000b s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000005 inst:  00138393 Time_Step:  0005
ra:  00000000 sp:  00000000 t0:  00000003 t1:  00000003 t2:  0000000b s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000006 inst:  00230313 Time_Step:  0006
ra:  00000000 sp:  00000000 t0:  00000003 t1:  00000003 t2:  0000000b s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000007 inst:  00000000 Time_Step:  0007
ra:  00000000 sp:  00000000 t0:  00000003 t1:  00000005 t2:  0000000b s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000008 inst:  00000000 Time_Step:  0008
""", traceString)
    }

    @Test
    fun branches3InstFirstTwoStageTest() {
        val ss = SimulatorSettings()
        ss.setRegesOnInit = false
        val sim = makeSim("""
        fib:
            beq x0, x0, done #6

        done:
            addi a0, x0, 1
            addi a0, a0, 2
            addi a0, a0, 3
        """, ss)
        val t = Tracer(sim)
        t.format = "ra:  %x1% sp:  %x2% t0:  %5% t1:  %6% t2:  %7% s0:  %8% s1:  %9% a0:  %10% PC:  %pc% inst:  %inst% Time_Step:  %line%\\n"
        t.instFirst = true
        wordAddressed = true
        t.base = 16
        t.twoStage = true
        val traceString = trace(t)
        assertEquals("""ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000000 inst:  00000263 Time_Step:  0000
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000001 inst:  00100513 Time_Step:  0001
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000001 inst:  00100513 Time_Step:  0002
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000002 inst:  00250513 Time_Step:  0003
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000001 PC:  00000003 inst:  00350513 Time_Step:  0004
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000003 PC:  00000004 inst:  00000000 Time_Step:  0005
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000006 PC:  00000005 inst:  00000000 Time_Step:  0006
""", traceString)
    }

    @Test
    fun branchEndInstFirstTwoStageTest() {
        val ss = SimulatorSettings()
        ss.setRegesOnInit = false
        val sim = makeSim("""
            add t0 x0 x0
            addi t1 x0 1
            blt t0 t1 8 
            
            addi t0 t0 1
            addi t1 t1 1
            
            blt t0 t1 8
        """, ss)
        val t = Tracer(sim)
        t.format = "ra:  %x1% sp:  %x2% t0:  %5% t1:  %6% t2:  %7% s0:  %8% s1:  %9% a0:  %10% PC:  %pc% inst:  %inst% Time_Step:  %line%\\n"
        t.instFirst = true
        wordAddressed = false
        t.base = 16
        t.twoStage = true
        val traceString = trace(t)
        assertEquals("""ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000000 inst:  000002b3 Time_Step:  0000
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000004 inst:  00100313 Time_Step:  0001
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000008 inst:  0062c463 Time_Step:  0002
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000001 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  0000000c inst:  00128293 Time_Step:  0003
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000001 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000010 inst:  00130313 Time_Step:  0004
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000001 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000014 inst:  0062c463 Time_Step:  0005
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000002 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000018 inst:  00000000 Time_Step:  0006
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000002 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  0000001c inst:  00000000 Time_Step:  0007
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000002 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000020 inst:  00000000 Time_Step:  0008
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000002 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000024 inst:  00000000 Time_Step:  0009
""", traceString)
    }

    @Test
    fun jumpEndInstFirstTwoStageTest() {
        val ss = SimulatorSettings()
        ss.setRegesOnInit = false
        val sim = makeSim("""
            addi x5 x0 20
            jalr x0 x5 0
        """, ss)
        val t = Tracer(sim)
        t.format = "ra:  %x1% sp:  %x2% t0:  %5% t1:  %6% t2:  %7% s0:  %8% s1:  %9% a0:  %10% PC:  %pc% inst:  %inst% Time_Step:  %line%\\n"
        t.instFirst = true
        wordAddressed = false
        t.base = 16
        t.twoStage = true
        val traceString = trace(t)
        assertEquals("""ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000000 inst:  01400293 Time_Step:  0000
ra:  00000000 sp:  00000000 t0:  00000000 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000004 inst:  00028067 Time_Step:  0001
ra:  00000000 sp:  00000000 t0:  00000014 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000008 inst:  00000000 Time_Step:  0002
ra:  00000000 sp:  00000000 t0:  00000014 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000014 inst:  00000000 Time_Step:  0003
ra:  00000000 sp:  00000000 t0:  00000014 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  00000018 inst:  00000000 Time_Step:  0004
ra:  00000000 sp:  00000000 t0:  00000014 t1:  00000000 t2:  00000000 s0:  00000000 s1:  00000000 a0:  00000000 PC:  0000001c inst:  00000000 Time_Step:  0005
""", traceString)
    }
}