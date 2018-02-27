package com.docustream

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.docustream.model.Container
import com.docustream.model.Example
import com.docustream.model.Priority
import com.docustream.model.SubItem
import com.docustream.model.TinyObject
import org.junit.Before

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Killian on 24/01/2018.
 */
@RunWith(AndroidJUnit4::class)
class DocuStreamTest {

    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getTargetContext()
        assertEquals("com.docustream.test", context.getPackageName())

        // Clear old settings
        val storage = DocuStream(context.applicationContext, rootType = Example::class.java)
        val resetSuccesfull = storage.reset()
        System.out.println("resetSuccesfull: $resetSuccesfull")
    }

    /* ********** [initialize] ********** */

    @Test(expected = IllegalArgumentException::class)
    fun a1_initializeWithWeakContext() {
        DocuStream(context, rootType = Example::class.java)

        // No assertion!
    }

    @Test
    fun a2_initializeWithStrongContext() {
        val storage = DocuStream(context.applicationContext, rootType = Example::class.java)
        assertNotNull(storage)
    }

    @Test
    fun a3_savedObjectShouldResultInSameObject() {
        val storage = DocuStream(context.applicationContext, rootType = Example::class.java)
        val createdInstance = storage.getData()

        Log.i("a3_savedObjectShouldResultInSameObject", createdInstance.toString())

        assertNotNull("Object should not be null", createdInstance)
    }

    @Test
    fun a4_savedObjectShouldResultInSameObject() {
        val storage = DocuStream(context.applicationContext, rootType = Example::class.java)
        val createdInstance = storage.getData()

        Log.i("a3_savedObjectShouldResultInSameObject", createdInstance.toString())

        assertNotNull("Object should not be null", createdInstance)
        assertEquals("default", createdInstance.name)
    }

    @Test
    fun a5_savedObjectShouldResultInSameObject() {
        val storage = DocuStream(context.applicationContext, rootType = Example::class.java)
        storage.setData(Example("bob", 30))

        val createdInstance = storage.getData()

        assertEquals("bob", createdInstance.name)
    }

    /* ********** [ Prevent storing wrong data ] ********** */

    @Test
    fun b1_preventStoringInvalidData() {
        val storage = DocuStream(context.applicationContext, rootType = Container::class.java)
        // storage.setData("") // <-- pre-compiler does not allow this test
    }

    /* ********** [ LARGE DATA STORAGE TESTS ] ********** */

    @Test
    fun c1_getDefaultValueFromBigObject() {
        val storage = DocuStream(context.applicationContext, rootType = Container::class.java)
        val container = storage.getData()

        Log.d("b1_getDefaultValueFromBigObject", container.toString())

        assertEquals(Priority.LOW, container.priority)
    }

    @Test
    fun c2_getDefaultValueFromBigObject() {
        val storage = DocuStream(context.applicationContext, rootType = Container::class.java)
        val container = storage.getData()

        if (container.items == null) {
            container.items = mutableListOf()
        }

        container.items?.let { items ->
            items.add(SubItem("body one", TinyObject()))
            items.add(SubItem("body two", TinyObject()))
            items.add(SubItem("body three", TinyObject()))
            items.add(SubItem("body four", TinyObject()))

            val fifthObject = TinyObject()
            fifthObject.count = 50
            items.add(SubItem("body five", fifthObject))
        }

        // persist this
        storage.setData(container)

        // get a new instance
        val newContainer = storage.getData()
        assertEquals(5, newContainer.items?.size)
        assertEquals(50, newContainer.items!![4].subsection?.count)
    }

    /* ********** [ HUGE DATA AMOUNT / PERFORMANCE ] ********** */

    // ~5 seconds for 1.000.000 items
    @Test(timeout = 6000)
    fun d1_getDefaultValueFromBigObject() {
        val storage = DocuStream(context.applicationContext, rootType = Container::class.java)
        val container = storage.getData()

        if (container.items == null) {
            Log.d("d1", "item list created.")
            container.items = mutableListOf()
        }

        container.items?.let { items ->
            for (i in 1..100000) {
                val item = SubItem(body = i.toString())
                items.add(item)
            }
        }

        storage.setData(container)

        val newInstance = storage.getData()
        assertEquals(100000, newInstance.items?.size)
    }

    /* ********** [ Change directory ] ********** */

//    @Test(timeout = 6000)
//    fun e1_handleCaseWithPermission() {
//        val storage = DocuStream(context.applicationContext, rootType = Container::class.java)
//
//    }



    }