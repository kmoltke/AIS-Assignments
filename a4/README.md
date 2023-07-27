# Assignment 4
_Kristian Moltke Reitzel, krei@itu.dk_

## Problem 1
### Part 1
```shell
gpg --decrypt --passphrase takethis symmetric.txt.gpg
```

```shell
echo "kristian" > mysymmetric.txt
```

```shell
gpg --symmetric  mysymmetric.txt
```

```shell
gpg --decrypt --passphrase takethis mysymmetric.txt.gpg
```

```shell
gpg --gen-key
```

```shell
gpg --import bob_pub.key
```

```shell
gpg --encrypt --sign --recipient 'Bob Ross'  mypublic.txt
```

```shell
gpg --output mypublickey.gpg --export gpgtest1@mailinator.com
```

### Part 2
![img.png](screenshots/img.png)
```shell
openssl pkcs12 -export -in paybud.crt -inkey paybud.key -out paybud.p12 -name paybud -caname root
```

### Part 3
When someone signs another person's key, they are essentially vouching for the authenticity of that person's identity. The act of signing someone's key implies that the signer has verified the identity of the key's owner and is asserting that the key indeed belongs to that person.

e.g.
55921c9468e36ab22a937d070e2b84019a7db7b0 signed for freysteinn@freysteinn.com

[selfsig] is a digital signature made by the key owner on their own key or User ID, which means that they sign for his own identity.

"revok" is short for "revoke," and it indicates that the corresponding key or signature has been revoked, which means that the owner of the key or the signer no longer vouches for the authenticity of the key or the data they signed

The Web of Trust is a decentralized trust model used in PGP and GnuPG to establish and verify the authenticity of public keys. In the Web of Trust, users can sign each other's keys to create a network of trust relationships. For example, Freysteinn Alfredsson freysteinn@freysteinn.com and Freysteinn Alfredsson freysteinn@1984.is have both signed each other's keys, creating a mutual trust relationship.

