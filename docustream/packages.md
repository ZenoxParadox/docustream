# Module docustream

Library that (de-)serializes any specified object using gson.

## Usage

In your application class simply create a reference to the docustream library. The most
minimal setup is something like the following;

```
val stream = DocuStream(this, rootType = User::class.java)
```

Here, the rootType can be any object you prefer. Typically this is a `User` or `Session` or
`Profile` (or anything you like).

From this point on it's just a matter of calling:

```
val user = stream.getData()
```

## Security
It supports encrypting the data in a secure way. What it does is it generates an RSA KeyPair in 
order to encrypt and decrypt keys used to encrypt/encrypt the data. Data is encrypted using AES 
encryption.

Additional to the above construction, you would supply an instance of a `DataCipher`.

```
val stream = DocuStream(this, rootType = User::class.java, cipher = DataCipher(this))
```

The rest of the encryption is done behind the scenes. Each time you store your data (using `setData(T)`)
the encryption is changed for you.

# Package com.docustream.encryption

The package containing ciphers. These will encrypt the data (the root object). You don't have to 
encrypt your data.

# Package com.docustream.common

Common objects that you _could_ use in your object. For example an object to store an oAuth token.