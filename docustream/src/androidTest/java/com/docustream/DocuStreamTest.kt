package com.docustream

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.docustream.encryption.DataCipher
import com.docustream.model.Container
import com.docustream.model.Example
import com.docustream.model.Priority
import com.docustream.model.Simple
import com.docustream.model.SubItem
import com.docustream.model.TinyObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import javax.crypto.KeyGenerator



/**
 * tests
 *
 * Created by Killian on 24/01/2018.
 */
@RunWith(AndroidJUnit4::class)
class DocuStreamTest {

    private val context = InstrumentationRegistry.getTargetContext()

    @Before
    fun setup() {
        Log.i("setup", "setup() ----------------------------------------")

        assertEquals("com.docustream.test", context.packageName)

//        // Clear old settings
//        val storageExample = DocuStream(context.applicationContext, rootType = Example::class.java)
//        val storageExampleReset = storageExample.reset()
//        Log.d("setup", "storageExampleReset: $storageExampleReset")
//
//        // Clear old settings
//        val storageContainer = DocuStream(context.applicationContext, rootType = Container::class.java)
//        val containerReset = storageContainer.reset()
//        Log.d("setup", "containerReset: $containerReset")
//
//        val cipher = DataCipher(context.applicationContext)
//        val removed = cipher.reset()
//        Log.d("setup", "cipher reset. Output: [$removed]")

        // Remove all files
        val directory = context.filesDir
        Log.v("setup", "directory [${directory.name}]")
        for (file in directory.listFiles()) {
            val name = file.name
            val removed = file.delete()
            Log.v("setup", "file [$name] removed [$removed]")
        }

        Log.i("setup", "---------------------------------------- setup()")
    }

    private fun LOG_TAG(method: String): String {
        return "DocuStreamTest-$method"
    }

    private fun fnumber(number: Int, size: Int = 2): String {
        return String.format("%0${size}d", number)
    }

    /* ********** [initialize] ********** */

    @Test(expected = IllegalArgumentException::class)
    fun a1_initializeWithWeakContext() {
        val storage = DocuStream(context, rootType = Example::class.java)

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

    /* ********** [ Encryption ] ********** */

//    @Ignore(value = "No longer relevant; the key is generated")
//    @Test(expected = IllegalArgumentException::class)
//    fun f1_minimalLength() {
//        // Both of these keys have an invalid length (needs multiple of 16)
//        DataCipher()
//
//        // No assertion!
//    }
//
//    @Ignore(value = "No longer relevant; the key is generated")
//    @Test(expected = IllegalArgumentException::class)
//    fun f1_noLength() {
//        // Both of these keys have an invalid length (needs multiple of 16)
//        DataCipher()
//
//        // No assertion!
//    }

    // https://dri.es/files/oopsla07-georges.pdf

    /*
    5.000 = ~100ms
    10.000 = 870ms
    // TODO check appropriate timeout
     */
    @Test(timeout = 15000)
    fun f2_performanceTestInitialisation() {
        var cipher: DataCipher? = null

        for (i in 1..250) {
            cipher = DataCipher(context.applicationContext)
        }

        Log.v(LOG_TAG("f2"), cipher.toString())

        assertTrue(true)
    }

    @Test
    fun f3_encryptAndDecryptProof() {
        val testValue = "lollerskates"

        val cipher = DataCipher(context.applicationContext)
        val bytes = cipher.generateVector()

        val encrypted = cipher.encrypt(testValue, bytes)
        Log.d(LOG_TAG("f3-encrypted"), encrypted)

        val decrypted = cipher.decrypt(encrypted, bytes)
        Log.d(LOG_TAG("f3-decrypted"), decrypted)

        assertEquals(testValue, decrypted)
    }

    @Test
    fun f4_performanceTestEncryption() {
        val json = "{\"menu\":{\"header\":\"SVG Viewer\",\"items\":[{\"id\":\"Open\"},{\"id\":\"OpenNew\",\"label\":\"Open New\"},null,{\"id\":\"ZoomIn\",\"label\":\"Zoom In\"},{\"id\":\"ZoomOut\",\"label\":\"Zoom Out\"},{\"id\":\"OriginalView\",\"label\":\"Original View\"},null,{\"id\":\"Quality\"},{\"id\":\"Pause\"},{\"id\":\"Mute\"},null,{\"id\":\"Find\",\"label\":\"Find...\"},{\"id\":\"FindAgain\",\"label\":\"Find Again\"},{\"id\":\"Copy\"},{\"id\":\"CopyAgain\",\"label\":\"Copy Again\"},{\"id\":\"CopySVG\",\"label\":\"Copy SVG\"},{\"id\":\"ViewSVG\",\"label\":\"View SVG\"},{\"id\":\"ViewSource\",\"label\":\"View Source\"},{\"id\":\"SaveAs\",\"label\":\"Save As\"},null,{\"id\":\"Help\"},{\"id\":\"About\",\"label\":\"About Adobe CVG Viewer...\"}]}}"
        val cipher = DataCipher(context.applicationContext)
        val bytes = cipher.generateVector()

        val encrypted = cipher.encrypt(json, bytes)
        Log.d(LOG_TAG("f4-encrypted"), encrypted)

        val decrypted = cipher.decrypt(encrypted, bytes)
        Log.d(LOG_TAG("f4-decrypted"), decrypted)

        assertEquals(json.length, decrypted.length)
    }

    @Test
    fun f5_performanceTestEncryption() {
        val json = "[{\"_id\":\"5a995b3477cbc0734a8d02bf\",\"index\":0,\"guid\":\"f8bfc6d2-1ced-42da-ac51-b1c4e14af457\",\"isActive\":true,\"balance\":\"\$1,598.29\",\"picture\":\"http://placehold.it/32x32\",\"age\":22,\"eyeColor\":\"green\",\"name\":{\"first\":\"Walls\",\"last\":\"Price\"},\"company\":\"KONGENE\",\"email\":\"walls.price@kongene.biz\",\"phone\":\"+1 (985) 505-3791\",\"address\":\"369 Denton Place, Oneida, Michigan, 4998\",\"about\":\"Ipsum deserunt dolor in aute est ut id velit reprehenderit. Cillum amet nisi enim sit consequat ad nulla. Reprehenderit magna ullamco non excepteur anim cupidatat reprehenderit velit nostrud adipisicing nostrud mollit consequat ut. Ipsum ad non aliqua reprehenderit ea consequat labore incididunt.\",\"registered\":\"Tuesday, December 8, 2015 12:21 PM\",\"latitude\":\"45.982962\",\"longitude\":\"-28.9524\",\"tags\":[\"adipisicing\",\"do\",\"mollit\",\"Lorem\",\"aute\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Maddox Roberts\"},{\"id\":1,\"name\":\"Diann Bates\"},{\"id\":2,\"name\":\"Beck Cardenas\"}],\"greeting\":\"Hello, Walls! You have 9 unread messages.\",\"favoriteFruit\":\"strawberry\"},{\"_id\":\"5a995b34b182d418eb3d0982\",\"index\":1,\"guid\":\"1631eccf-26ae-42f7-b292-f1cb4d5dc24b\",\"isActive\":true,\"balance\":\"\$2,693.04\",\"picture\":\"http://placehold.it/32x32\",\"age\":30,\"eyeColor\":\"brown\",\"name\":{\"first\":\"Pamela\",\"last\":\"Guthrie\"},\"company\":\"CONCILITY\",\"email\":\"pamela.guthrie@concility.tv\",\"phone\":\"+1 (811) 445-2017\",\"address\":\"448 Ridgewood Avenue, Eden, Illinois, 706\",\"about\":\"Laborum proident sit officia aliquip occaecat. Commodo nulla in nostrud proident incididunt pariatur laborum reprehenderit incididunt est. Nisi aute Lorem nulla consequat ullamco cillum mollit eiusmod ea consectetur quis laboris.\",\"registered\":\"Monday, January 1, 2018 10:01 AM\",\"latitude\":\"-8.605299\",\"longitude\":\"156.18338\",\"tags\":[\"consectetur\",\"dolore\",\"elit\",\"nisi\",\"in\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Christi Stephens\"},{\"id\":1,\"name\":\"Conway Frye\"},{\"id\":2,\"name\":\"Durham Austin\"}],\"greeting\":\"Hello, Pamela! You have 10 unread messages.\",\"favoriteFruit\":\"strawberry\"},{\"_id\":\"5a995b3492d4a4ef90b26ab2\",\"index\":2,\"guid\":\"40a0ae3f-bd05-4ac1-9f1e-735541e6db65\",\"isActive\":false,\"balance\":\"\$3,665.03\",\"picture\":\"http://placehold.it/32x32\",\"age\":32,\"eyeColor\":\"green\",\"name\":{\"first\":\"Cherry\",\"last\":\"Barker\"},\"company\":\"ZOID\",\"email\":\"cherry.barker@zoid.co.uk\",\"phone\":\"+1 (811) 544-3964\",\"address\":\"400 Croton Loop, Frank, Alabama, 2553\",\"about\":\"Fugiat nisi Lorem cupidatat est do ea irure ad ex aliquip sit culpa excepteur. Cupidatat officia cupidatat exercitation velit sit velit voluptate aliquip anim cillum ullamco tempor. Ad ullamco exercitation sint nulla anim veniam eu sint excepteur minim excepteur. Exercitation sit cillum in do deserunt ad et reprehenderit sint. Incididunt do consequat eu ad pariatur adipisicing. Cupidatat dolore officia nisi non anim sit irure non.\",\"registered\":\"Thursday, February 11, 2016 6:54 PM\",\"latitude\":\"14.256005\",\"longitude\":\"85.397976\",\"tags\":[\"dolor\",\"aute\",\"ea\",\"ipsum\",\"mollit\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Lucinda Mcdowell\"},{\"id\":1,\"name\":\"Felecia Peterson\"},{\"id\":2,\"name\":\"Audra Hopper\"}],\"greeting\":\"Hello, Cherry! You have 5 unread messages.\",\"favoriteFruit\":\"banana\"},{\"_id\":\"5a995b34a6abb495986a1e91\",\"index\":3,\"guid\":\"7358290e-0c33-417d-8d08-8b5d25559282\",\"isActive\":true,\"balance\":\"\$3,732.53\",\"picture\":\"http://placehold.it/32x32\",\"age\":34,\"eyeColor\":\"blue\",\"name\":{\"first\":\"Cara\",\"last\":\"Bridges\"},\"company\":\"QUARX\",\"email\":\"cara.bridges@quarx.me\",\"phone\":\"+1 (975) 520-2433\",\"address\":\"873 Vandervoort Place, Caroleen, Marshall Islands, 1018\",\"about\":\"In nulla qui non reprehenderit Lorem proident et excepteur dolore. Et elit aliquip ut veniam velit adipisicing amet ut ullamco in magna enim anim. Veniam ex tempor labore enim pariatur commodo nulla consequat enim sunt ipsum magna est commodo. Lorem exercitation irure labore nostrud ipsum duis nisi anim reprehenderit consequat ad. Labore amet labore mollit pariatur ad ad commodo labore labore do sunt sit adipisicing excepteur. Elit sint consequat esse incididunt sit occaecat cupidatat duis eiusmod aliqua duis ullamco. Adipisicing veniam aliqua nulla duis id exercitation ea sint qui sit do.\",\"registered\":\"Tuesday, March 11, 2014 8:52 PM\",\"latitude\":\"-81.365614\",\"longitude\":\"-51.777886\",\"tags\":[\"eiusmod\",\"labore\",\"exercitation\",\"irure\",\"quis\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Aguirre Shepherd\"},{\"id\":1,\"name\":\"Cassandra Leon\"},{\"id\":2,\"name\":\"Keller Davis\"}],\"greeting\":\"Hello, Cara! You have 10 unread messages.\",\"favoriteFruit\":\"banana\"},{\"_id\":\"5a995b3461d03529b8c7f5cf\",\"index\":4,\"guid\":\"fb518e40-2f07-4c13-93fc-3a5c92dbdd6b\",\"isActive\":false,\"balance\":\"\$1,034.76\",\"picture\":\"http://placehold.it/32x32\",\"age\":33,\"eyeColor\":\"blue\",\"name\":{\"first\":\"Sykes\",\"last\":\"Schwartz\"},\"company\":\"WARETEL\",\"email\":\"sykes.schwartz@waretel.info\",\"phone\":\"+1 (816) 566-3843\",\"address\":\"385 Bethel Loop, Somerset, Nevada, 7578\",\"about\":\"Deserunt minim exercitation irure proident veniam. Irure adipisicing exercitation minim enim deserunt nisi nulla. Proident ullamco sint aute nulla. Ut ea ullamco labore amet magna aute Lorem magna elit dolor incididunt id velit.\",\"registered\":\"Monday, February 1, 2016 5:22 AM\",\"latitude\":\"51.648753\",\"longitude\":\"51.554287\",\"tags\":[\"fugiat\",\"duis\",\"culpa\",\"deserunt\",\"dolore\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Emma Holder\"},{\"id\":1,\"name\":\"Maynard Soto\"},{\"id\":2,\"name\":\"Sutton Bradshaw\"}],\"greeting\":\"Hello, Sykes! You have 8 unread messages.\",\"favoriteFruit\":\"apple\"},{\"_id\":\"5a995b34e401c41386b76737\",\"index\":5,\"guid\":\"71941ca7-b800-46c5-8305-db3311742cc4\",\"isActive\":true,\"balance\":\"\$1,361.82\",\"picture\":\"http://placehold.it/32x32\",\"age\":40,\"eyeColor\":\"blue\",\"name\":{\"first\":\"Violet\",\"last\":\"Torres\"},\"company\":\"IPLAX\",\"email\":\"violet.torres@iplax.us\",\"phone\":\"+1 (963) 563-2407\",\"address\":\"953 Whitwell Place, Wawona, New York, 7499\",\"about\":\"Fugiat velit labore laborum aliqua tempor. Tempor Lorem cupidatat pariatur veniam amet anim laborum eu excepteur consectetur ut ea eu excepteur. Ipsum laborum nisi sunt sit est laborum ea sunt consectetur.\",\"registered\":\"Sunday, July 6, 2014 7:55 AM\",\"latitude\":\"18.405846\",\"longitude\":\"155.7978\",\"tags\":[\"mollit\",\"eu\",\"excepteur\",\"ad\",\"laborum\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Whitaker Powers\"},{\"id\":1,\"name\":\"Alicia Hunter\"},{\"id\":2,\"name\":\"Torres Morton\"}],\"greeting\":\"Hello, Violet! You have 10 unread messages.\",\"favoriteFruit\":\"strawberry\"},{\"_id\":\"5a995b347de5446d2e8a9fe5\",\"index\":6,\"guid\":\"4483b367-45fd-425a-967a-a950d1d1ad65\",\"isActive\":true,\"balance\":\"\$2,605.69\",\"picture\":\"http://placehold.it/32x32\",\"age\":25,\"eyeColor\":\"blue\",\"name\":{\"first\":\"Florence\",\"last\":\"Key\"},\"company\":\"DIGITALUS\",\"email\":\"florence.key@digitalus.org\",\"phone\":\"+1 (859) 503-3225\",\"address\":\"824 Portland Avenue, Stewart, Virginia, 3846\",\"about\":\"In aliqua commodo Lorem ea laborum aute do anim id ad ipsum quis. Eiusmod excepteur dolore id est id consectetur nostrud id ut velit dolor sunt aliqua minim. Sunt nostrud cupidatat quis amet cillum pariatur sunt minim. Id non labore aliquip anim sint dolore tempor enim anim ipsum est aliquip enim.\",\"registered\":\"Saturday, March 25, 2017 11:21 PM\",\"latitude\":\"-2.99417\",\"longitude\":\"6.98633\",\"tags\":[\"adipisicing\",\"exercitation\",\"sunt\",\"laboris\",\"consequat\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Kelsey Benjamin\"},{\"id\":1,\"name\":\"Guerrero Cline\"},{\"id\":2,\"name\":\"Eva Mills\"}],\"greeting\":\"Hello, Florence! You have 8 unread messages.\",\"favoriteFruit\":\"banana\"},{\"_id\":\"5a995b3406c1f59437987d27\",\"index\":7,\"guid\":\"75e165a3-8d8b-4f53-9c72-9ca3d7ee221c\",\"isActive\":false,\"balance\":\"\$2,247.15\",\"picture\":\"http://placehold.it/32x32\",\"age\":26,\"eyeColor\":\"brown\",\"name\":{\"first\":\"Gallagher\",\"last\":\"Brock\"},\"company\":\"TALENDULA\",\"email\":\"gallagher.brock@talendula.com\",\"phone\":\"+1 (804) 418-3126\",\"address\":\"914 Arion Place, Tryon, District Of Columbia, 4651\",\"about\":\"Nisi eiusmod aliquip mollit aliquip ullamco qui anim et fugiat in ut nisi nisi. Dolore aute cupidatat commodo minim labore in incididunt irure exercitation elit ex Lorem minim. Lorem ullamco aute nisi exercitation in irure eu ex reprehenderit. Nisi ea sunt sint fugiat enim eiusmod enim ad nostrud non do aliquip Lorem. Velit duis reprehenderit qui ad amet consectetur anim enim deserunt tempor ea elit Lorem. Aliqua velit consectetur duis Lorem magna adipisicing in qui anim esse amet irure aliquip dolor. Id quis aliqua aliqua ullamco labore non consectetur irure ex magna quis.\",\"registered\":\"Monday, June 27, 2016 1:01 AM\",\"latitude\":\"-84.052988\",\"longitude\":\"115.213747\",\"tags\":[\"irure\",\"laborum\",\"officia\",\"fugiat\",\"ad\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Sims Howell\"},{\"id\":1,\"name\":\"Stevenson Mooney\"},{\"id\":2,\"name\":\"Massey Coffey\"}],\"greeting\":\"Hello, Gallagher! You have 7 unread messages.\",\"favoriteFruit\":\"apple\"}]"
        val cipher = DataCipher(context.applicationContext)
        val bytes = cipher.generateVector()

        val encrypted = cipher.encrypt(json, bytes)
        Log.d(LOG_TAG("f5-encrypted"), encrypted)

        val decrypted = cipher.decrypt(encrypted, bytes)
        Log.d(LOG_TAG("f5-decrypted"), decrypted)

        assertEquals(json.length, decrypted.length)
    }

    /* ********** [ Initialize encryption ] ********** */

    // copy of other (above) unit test, but now with encryption

    @Test
    fun g0_encryptDecrypt() {
        val cipher = DataCipher(context.applicationContext)
        val key = "1234567890"

        val encryptedKey = cipher.encryptKey(key)
        val decryptedValue = cipher.decryptKey(encryptedKey)

        assertEquals(key, decryptedValue)
    }

    @Test
    fun g1_encryptEmptyObject() {
        val cipher = DataCipher(context.applicationContext)
        val storage = DocuStream(context.applicationContext, cipher = cipher, rootType = Example::class.java)

        val createdInstance = storage.getData()

        assertEquals("default", createdInstance.name)

        Log.v(LOG_TAG("g2"), storage.getFileContents())
    }

    @Test
    fun g2_encryptSmallObject() {
        // Note we're not changing the [Example.variable] variable
        val testObject = Example("david", 30)

        val cipher = DataCipher(context.applicationContext)
        val storage = DocuStream(context.applicationContext, cipher = cipher, rootType = Example::class.java)
        storage.setData(testObject)

        val newInstance = storage.getData()

        assertEquals("david", newInstance.name)
        assertEquals(30, newInstance.age)
        assertEquals("this value is from the default file (unchanged and untouched)", newInstance.variable)

        Log.v(LOG_TAG("g2"), storage.getFileContents())
    }

    @Test
    fun g3_encryptLargeObject() {
        val cipher = DataCipher(context.applicationContext)
        val storage = DocuStream(context.applicationContext, cipher = cipher, rootType = Container::class.java)
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

        Log.v(LOG_TAG("g3"), storage.getFileContents())
    }

    @Test
    fun g4_performanceTestEncryption() {
        val json = "[{\"_id\":\"5a995b3477cbc0734a8d02bf\",\"index\":0,\"guid\":\"f8bfc6d2-1ced-42da-ac51-b1c4e14af457\",\"isActive\":true,\"balance\":\"\$1,598.29\",\"picture\":\"http://placehold.it/32x32\",\"age\":22,\"eyeColor\":\"green\",\"name\":{\"first\":\"Walls\",\"last\":\"Price\"},\"company\":\"KONGENE\",\"email\":\"walls.price@kongene.biz\",\"phone\":\"+1 (985) 505-3791\",\"address\":\"369 Denton Place, Oneida, Michigan, 4998\",\"about\":\"Ipsum deserunt dolor in aute est ut id velit reprehenderit. Cillum amet nisi enim sit consequat ad nulla. Reprehenderit magna ullamco non excepteur anim cupidatat reprehenderit velit nostrud adipisicing nostrud mollit consequat ut. Ipsum ad non aliqua reprehenderit ea consequat labore incididunt.\",\"registered\":\"Tuesday, December 8, 2015 12:21 PM\",\"latitude\":\"45.982962\",\"longitude\":\"-28.9524\",\"tags\":[\"adipisicing\",\"do\",\"mollit\",\"Lorem\",\"aute\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Maddox Roberts\"},{\"id\":1,\"name\":\"Diann Bates\"},{\"id\":2,\"name\":\"Beck Cardenas\"}],\"greeting\":\"Hello, Walls! You have 9 unread messages.\",\"favoriteFruit\":\"strawberry\"},{\"_id\":\"5a995b34b182d418eb3d0982\",\"index\":1,\"guid\":\"1631eccf-26ae-42f7-b292-f1cb4d5dc24b\",\"isActive\":true,\"balance\":\"\$2,693.04\",\"picture\":\"http://placehold.it/32x32\",\"age\":30,\"eyeColor\":\"brown\",\"name\":{\"first\":\"Pamela\",\"last\":\"Guthrie\"},\"company\":\"CONCILITY\",\"email\":\"pamela.guthrie@concility.tv\",\"phone\":\"+1 (811) 445-2017\",\"address\":\"448 Ridgewood Avenue, Eden, Illinois, 706\",\"about\":\"Laborum proident sit officia aliquip occaecat. Commodo nulla in nostrud proident incididunt pariatur laborum reprehenderit incididunt est. Nisi aute Lorem nulla consequat ullamco cillum mollit eiusmod ea consectetur quis laboris.\",\"registered\":\"Monday, January 1, 2018 10:01 AM\",\"latitude\":\"-8.605299\",\"longitude\":\"156.18338\",\"tags\":[\"consectetur\",\"dolore\",\"elit\",\"nisi\",\"in\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Christi Stephens\"},{\"id\":1,\"name\":\"Conway Frye\"},{\"id\":2,\"name\":\"Durham Austin\"}],\"greeting\":\"Hello, Pamela! You have 10 unread messages.\",\"favoriteFruit\":\"strawberry\"},{\"_id\":\"5a995b3492d4a4ef90b26ab2\",\"index\":2,\"guid\":\"40a0ae3f-bd05-4ac1-9f1e-735541e6db65\",\"isActive\":false,\"balance\":\"\$3,665.03\",\"picture\":\"http://placehold.it/32x32\",\"age\":32,\"eyeColor\":\"green\",\"name\":{\"first\":\"Cherry\",\"last\":\"Barker\"},\"company\":\"ZOID\",\"email\":\"cherry.barker@zoid.co.uk\",\"phone\":\"+1 (811) 544-3964\",\"address\":\"400 Croton Loop, Frank, Alabama, 2553\",\"about\":\"Fugiat nisi Lorem cupidatat est do ea irure ad ex aliquip sit culpa excepteur. Cupidatat officia cupidatat exercitation velit sit velit voluptate aliquip anim cillum ullamco tempor. Ad ullamco exercitation sint nulla anim veniam eu sint excepteur minim excepteur. Exercitation sit cillum in do deserunt ad et reprehenderit sint. Incididunt do consequat eu ad pariatur adipisicing. Cupidatat dolore officia nisi non anim sit irure non.\",\"registered\":\"Thursday, February 11, 2016 6:54 PM\",\"latitude\":\"14.256005\",\"longitude\":\"85.397976\",\"tags\":[\"dolor\",\"aute\",\"ea\",\"ipsum\",\"mollit\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Lucinda Mcdowell\"},{\"id\":1,\"name\":\"Felecia Peterson\"},{\"id\":2,\"name\":\"Audra Hopper\"}],\"greeting\":\"Hello, Cherry! You have 5 unread messages.\",\"favoriteFruit\":\"banana\"},{\"_id\":\"5a995b34a6abb495986a1e91\",\"index\":3,\"guid\":\"7358290e-0c33-417d-8d08-8b5d25559282\",\"isActive\":true,\"balance\":\"\$3,732.53\",\"picture\":\"http://placehold.it/32x32\",\"age\":34,\"eyeColor\":\"blue\",\"name\":{\"first\":\"Cara\",\"last\":\"Bridges\"},\"company\":\"QUARX\",\"email\":\"cara.bridges@quarx.me\",\"phone\":\"+1 (975) 520-2433\",\"address\":\"873 Vandervoort Place, Caroleen, Marshall Islands, 1018\",\"about\":\"In nulla qui non reprehenderit Lorem proident et excepteur dolore. Et elit aliquip ut veniam velit adipisicing amet ut ullamco in magna enim anim. Veniam ex tempor labore enim pariatur commodo nulla consequat enim sunt ipsum magna est commodo. Lorem exercitation irure labore nostrud ipsum duis nisi anim reprehenderit consequat ad. Labore amet labore mollit pariatur ad ad commodo labore labore do sunt sit adipisicing excepteur. Elit sint consequat esse incididunt sit occaecat cupidatat duis eiusmod aliqua duis ullamco. Adipisicing veniam aliqua nulla duis id exercitation ea sint qui sit do.\",\"registered\":\"Tuesday, March 11, 2014 8:52 PM\",\"latitude\":\"-81.365614\",\"longitude\":\"-51.777886\",\"tags\":[\"eiusmod\",\"labore\",\"exercitation\",\"irure\",\"quis\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Aguirre Shepherd\"},{\"id\":1,\"name\":\"Cassandra Leon\"},{\"id\":2,\"name\":\"Keller Davis\"}],\"greeting\":\"Hello, Cara! You have 10 unread messages.\",\"favoriteFruit\":\"banana\"},{\"_id\":\"5a995b3461d03529b8c7f5cf\",\"index\":4,\"guid\":\"fb518e40-2f07-4c13-93fc-3a5c92dbdd6b\",\"isActive\":false,\"balance\":\"\$1,034.76\",\"picture\":\"http://placehold.it/32x32\",\"age\":33,\"eyeColor\":\"blue\",\"name\":{\"first\":\"Sykes\",\"last\":\"Schwartz\"},\"company\":\"WARETEL\",\"email\":\"sykes.schwartz@waretel.info\",\"phone\":\"+1 (816) 566-3843\",\"address\":\"385 Bethel Loop, Somerset, Nevada, 7578\",\"about\":\"Deserunt minim exercitation irure proident veniam. Irure adipisicing exercitation minim enim deserunt nisi nulla. Proident ullamco sint aute nulla. Ut ea ullamco labore amet magna aute Lorem magna elit dolor incididunt id velit.\",\"registered\":\"Monday, February 1, 2016 5:22 AM\",\"latitude\":\"51.648753\",\"longitude\":\"51.554287\",\"tags\":[\"fugiat\",\"duis\",\"culpa\",\"deserunt\",\"dolore\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Emma Holder\"},{\"id\":1,\"name\":\"Maynard Soto\"},{\"id\":2,\"name\":\"Sutton Bradshaw\"}],\"greeting\":\"Hello, Sykes! You have 8 unread messages.\",\"favoriteFruit\":\"apple\"},{\"_id\":\"5a995b34e401c41386b76737\",\"index\":5,\"guid\":\"71941ca7-b800-46c5-8305-db3311742cc4\",\"isActive\":true,\"balance\":\"\$1,361.82\",\"picture\":\"http://placehold.it/32x32\",\"age\":40,\"eyeColor\":\"blue\",\"name\":{\"first\":\"Violet\",\"last\":\"Torres\"},\"company\":\"IPLAX\",\"email\":\"violet.torres@iplax.us\",\"phone\":\"+1 (963) 563-2407\",\"address\":\"953 Whitwell Place, Wawona, New York, 7499\",\"about\":\"Fugiat velit labore laborum aliqua tempor. Tempor Lorem cupidatat pariatur veniam amet anim laborum eu excepteur consectetur ut ea eu excepteur. Ipsum laborum nisi sunt sit est laborum ea sunt consectetur.\",\"registered\":\"Sunday, July 6, 2014 7:55 AM\",\"latitude\":\"18.405846\",\"longitude\":\"155.7978\",\"tags\":[\"mollit\",\"eu\",\"excepteur\",\"ad\",\"laborum\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Whitaker Powers\"},{\"id\":1,\"name\":\"Alicia Hunter\"},{\"id\":2,\"name\":\"Torres Morton\"}],\"greeting\":\"Hello, Violet! You have 10 unread messages.\",\"favoriteFruit\":\"strawberry\"},{\"_id\":\"5a995b347de5446d2e8a9fe5\",\"index\":6,\"guid\":\"4483b367-45fd-425a-967a-a950d1d1ad65\",\"isActive\":true,\"balance\":\"\$2,605.69\",\"picture\":\"http://placehold.it/32x32\",\"age\":25,\"eyeColor\":\"blue\",\"name\":{\"first\":\"Florence\",\"last\":\"Key\"},\"company\":\"DIGITALUS\",\"email\":\"florence.key@digitalus.org\",\"phone\":\"+1 (859) 503-3225\",\"address\":\"824 Portland Avenue, Stewart, Virginia, 3846\",\"about\":\"In aliqua commodo Lorem ea laborum aute do anim id ad ipsum quis. Eiusmod excepteur dolore id est id consectetur nostrud id ut velit dolor sunt aliqua minim. Sunt nostrud cupidatat quis amet cillum pariatur sunt minim. Id non labore aliquip anim sint dolore tempor enim anim ipsum est aliquip enim.\",\"registered\":\"Saturday, March 25, 2017 11:21 PM\",\"latitude\":\"-2.99417\",\"longitude\":\"6.98633\",\"tags\":[\"adipisicing\",\"exercitation\",\"sunt\",\"laboris\",\"consequat\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Kelsey Benjamin\"},{\"id\":1,\"name\":\"Guerrero Cline\"},{\"id\":2,\"name\":\"Eva Mills\"}],\"greeting\":\"Hello, Florence! You have 8 unread messages.\",\"favoriteFruit\":\"banana\"},{\"_id\":\"5a995b3406c1f59437987d27\",\"index\":7,\"guid\":\"75e165a3-8d8b-4f53-9c72-9ca3d7ee221c\",\"isActive\":false,\"balance\":\"\$2,247.15\",\"picture\":\"http://placehold.it/32x32\",\"age\":26,\"eyeColor\":\"brown\",\"name\":{\"first\":\"Gallagher\",\"last\":\"Brock\"},\"company\":\"TALENDULA\",\"email\":\"gallagher.brock@talendula.com\",\"phone\":\"+1 (804) 418-3126\",\"address\":\"914 Arion Place, Tryon, District Of Columbia, 4651\",\"about\":\"Nisi eiusmod aliquip mollit aliquip ullamco qui anim et fugiat in ut nisi nisi. Dolore aute cupidatat commodo minim labore in incididunt irure exercitation elit ex Lorem minim. Lorem ullamco aute nisi exercitation in irure eu ex reprehenderit. Nisi ea sunt sint fugiat enim eiusmod enim ad nostrud non do aliquip Lorem. Velit duis reprehenderit qui ad amet consectetur anim enim deserunt tempor ea elit Lorem. Aliqua velit consectetur duis Lorem magna adipisicing in qui anim esse amet irure aliquip dolor. Id quis aliqua aliqua ullamco labore non consectetur irure ex magna quis.\",\"registered\":\"Monday, June 27, 2016 1:01 AM\",\"latitude\":\"-84.052988\",\"longitude\":\"115.213747\",\"tags\":[\"irure\",\"laborum\",\"officia\",\"fugiat\",\"ad\"],\"range\":[0,1,2,3,4,5,6,7,8,9],\"friends\":[{\"id\":0,\"name\":\"Sims Howell\"},{\"id\":1,\"name\":\"Stevenson Mooney\"},{\"id\":2,\"name\":\"Massey Coffey\"}],\"greeting\":\"Hello, Gallagher! You have 7 unread messages.\",\"favoriteFruit\":\"apple\"}]"

        val cipher = DataCipher(context.applicationContext)
        val storage = DocuStream(context.applicationContext, cipher = cipher, rootType = String::class.java)

        storage.setData(json)

        val rawEncrypted = storage.getFileContents()
        Log.d(LOG_TAG("g4-rawEncrypted"), rawEncrypted)

        val rawDecrypted = storage.getData()

        assertEquals(json, rawDecrypted)
    }

    @Test
    fun g5_restoreSecretKey() {

        val cipher = DataCipher(context.applicationContext)
        val storage = DocuStream(context.applicationContext, cipher = cipher, rootType = Example::class.java)

        val data = storage.getData()
        data.variable = "changed to something else"
        storage.setData(data)

        // Pretend it's a new session!

        val otherCipher = DataCipher(context.applicationContext)
        val otherStorage = DocuStream(context.applicationContext, cipher = otherCipher, rootType = Example::class.java)

        val restoredData = otherStorage.getData()

        assertEquals(data.variable, restoredData.variable)
    }

    @Test
    fun g6_storeMultipleTimesWithDifferentContent() {

        val cipher = DataCipher(context.applicationContext)
        val storage = DocuStream(context.applicationContext, cipher = cipher, rootType = Example::class.java)

        val data = storage.getData()

        data.variable = "first"
        storage.setData(data)
        Log.v(LOG_TAG("g6-${fnumber(1)}"), storage.getFileContents())

        data.variable = "second"
        storage.setData(data)
        Log.v(LOG_TAG("g6-${fnumber(2)}"), storage.getFileContents())

        data.variable = "third"
        storage.setData(data)
        Log.v(LOG_TAG("g6-${fnumber(3)}"), storage.getFileContents())

        data.variable = "fourth"
        storage.setData(data)
        Log.v(LOG_TAG("g6-${fnumber(4)}"), storage.getFileContents())

        data.variable = "fifth"
        storage.setData(data)
        Log.v(LOG_TAG("g6-${fnumber(5)}"), storage.getFileContents())

        // Check if all the data is as expected
        val restoredData = storage.getData()
        assertEquals("default", restoredData.name)
        assertEquals(-1, restoredData.age)
        assertEquals("fifth", restoredData.variable)
    }

    @Test
    fun g7_storeMultipleTimesWithSameContent() {

        val encryptedValues = mutableSetOf<String>()
        var foundIssues = false

        val cipher = DataCipher(context.applicationContext)
        val storage = DocuStream(context.applicationContext, cipher = cipher, rootType = Example::class.java)

        val data = storage.getData()

        for(i in 0..20){
            storage.setData(data)
            val rawData = storage.getFileContents()
            Log.v(LOG_TAG("g7-${fnumber(i)}"), rawData)

            if (encryptedValues.contains(rawData)) {
                Log.e(LOG_TAG("g7-${fnumber(i)}"), "encountered a duplicate [$rawData]")
                foundIssues = true
            }
            encryptedValues.add(rawData)
        }

        // Check if all the data is as expected
        val restoredData = storage.getData()
        assertEquals("default", restoredData.name)
        assertEquals(-1, restoredData.age)
        assertEquals("this value is from the default file (unchanged and untouched)", restoredData.variable)

        // Check if we did not encounter the same encrypted value
        assertFalse(foundIssues)
    }

    @Test
    fun g8_testBlocksizeLimit() {
        val digits = "1234567890 "

        val cipher = DataCipher(context.applicationContext)
        val storage = DocuStream(context.applicationContext, cipher = cipher, rootType = Simple::class.java)

        val data = storage.getData()

        for (i in 0..1_000_000 step 100_000) {
            data.contents = digits.repeat(i)
            val plainSize = data.contents.length
            storage.setData(data)

            val blockSize = storage.getFileContents().length
            Log.v(LOG_TAG("g8-plainSize-blockSize-${fnumber(i, size = 2)}"), "$plainSize vs. $blockSize")
        }

        assertTrue(true)
    }

    /* ********** [ Initialize encryption ] ********** */

    @Ignore("NoClassDefFoundError")
    @Test
    fun h1_randomSecret() {

        // API 19:
        val keySpecBuilder = KeyGenParameterSpec.Builder("alias", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        keySpecBuilder.setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        keySpecBuilder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES)
        keyGenerator.init(keySpecBuilder.build())

        for(i in 0..100) {
            val key = keyGenerator.generateKey()
            Log.d(LOG_TAG("h1-key"), "key: $key")
        }

    }

    @Test
    fun h2_random() {
        val cipher = DataCipher(context.applicationContext)
        var issue = false

        for (i in 0..25) {
            val iteration = fnumber(i)
            val raw = i.toString()
            val bytes = cipher.generateVector()

            val encrypted = cipher.encrypt(raw, bytes)
            val decrypted = cipher.decrypt(encrypted, bytes)

            Log.d(LOG_TAG("h2-raw-encrypted-decrypted-$iteration"), "$raw->$decrypted. encrypted: $encrypted")

            if (!raw.contentEquals(decrypted)) {
                issue = true
            }
        }

        assertFalse(issue)
    }

    @Test
    fun h3_generatePrivateKey() {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(16)
        val privateKey = generator.generateKey()

        // This will create and save key to KeyStore
        Log.d(LOG_TAG("h2-privateKey"), "$privateKey  (encoded: ${privateKey.encoded})")
    }

    @Test
    fun h4_random() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val aliases = keyStore.aliases()
        var count = 0

        for (alias in aliases) {
            Log.d(LOG_TAG("h4-alias"), alias)
            count++
        }

        if (count == 0) {
            Log.w(LOG_TAG("h4-alias"), "no aliases")
        }
    }

    @Test
    fun h5_storeAndRetrieveValueFromKeyStore() {
        val keyAlias = "FILE_ENCRYPTION"

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null) // load package
        Log.d(LOG_TAG("h5-keyStore.size"), "${keyStore.size()}")

        val generator = KeyGenerator.getInstance("AES")
        generator.init(265)
        val secretKey = generator.generateKey()
        Log.d(LOG_TAG("h5-secretKey"), "${secretKey.encoded}")

        // ----- setEntry(String alias, Entry entry, ProtectionParameter)
        keyStore.setEntry(keyAlias, KeyStore.SecretKeyEntry(secretKey), null)

        // ----- setKeyEntry(alias, byte[] key, Certificate[] chain)
        //keyStore.setKeyEntry(keyAlias, secretKey.encoded, null)
        val hasAlias = keyStore.containsAlias(keyAlias)
        Log.d(LOG_TAG("h5-hasAlias"), "$hasAlias")

        // ----- setKeyEntry(String alias, Key key, char[] password, Certificate[] chain)

        val entry = keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry
        val secretKeyLoaded = entry.secretKey
        Log.d(LOG_TAG("h5-secretKeyLoaded"), "${secretKeyLoaded.encoded}")

        //val decoded = Base64.encodeToString(entry.secretKey.encoded, Base64.DEFAULT)
        //Log.d(LOG_TAG("h5-decoded"), decoded)

        val encodedEquals = secretKey.encoded.contentEquals(secretKeyLoaded.encoded)
        Log.d(LOG_TAG("h5-encodedEquals"), "$encodedEquals")

        assertEquals(secretKey, secretKeyLoaded)
        assertArrayEquals(secretKey.encoded, entry.secretKey.encoded)
    }

    @Test
    fun h6_generatedKeyDiffersEachTime() {
        val cipher = DataCipher(context.applicationContext)
        var issue = false

        for (i in 0..100) {
            val iteration = fnumber(i, 3)
            val raw = i.toString()
            val bytes = cipher.generateVector()

            val encrypted = cipher.encrypt(raw, bytes)
            val decrypted = cipher.decrypt(encrypted, bytes)

            Log.d(LOG_TAG("h6-raw-encrypted-decrypted-$iteration"), "$raw->$decrypted. encrypted=$encrypted")

            if (!decrypted.contentEquals(raw)) {
                issue = true
            }
        }

        assertFalse(issue)
    }

    @Test
    fun h7_encryptedValuesDiffer() {
        val cipher = DataCipher(context.applicationContext)
        val encryptedValues = mutableSetOf<String>()
        var foundIssues = false

        val valueToEncrypt = "example example exe"

        for (i in 0..100) {
            val iteration = fnumber(i, 3)
            val bytes = cipher.generateVector()

            val encrypted = cipher.encrypt(valueToEncrypt, bytes)
            Log.d(LOG_TAG("h7-encrypted-$iteration"), encrypted)

            if (encryptedValues.contains(encrypted)) {
                foundIssues = true
            }
            encryptedValues.add(encrypted)
        }

        assertEquals(101, encryptedValues.size)
        assertFalse(foundIssues)
    }


}