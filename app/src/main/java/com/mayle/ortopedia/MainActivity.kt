package com.mayle.ortopedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mayle.ortopedia.ui.theme.MayLeOrtopediaTheme
import java.util.Locale

data class Producto(
    val id: String = "",
    val codigo: String = "",
    val nombre: String = "",
    val precioContado: Double = 0.0,
    val precioUnPago: Double = 0.0,
    val precioTresCuotas: Double = 0.0,
    val stockTalle1: Long = 0,
    val stockTalle2: Long = 0,
    val stockTalle3: Long = 0
)

fun formatoPrecio(valor: Double): String {
    return if (valor % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", valor)
    } else {
        String.format(Locale.US, "%.2f", valor)
            .trimEnd('0')
            .trimEnd('.')
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MayLeOrtopediaTheme {
                MayLeApp()
            }
        }
    }
}

@Composable
fun MayLeApp() {

    var usuarioIngresado by remember {
        mutableStateOf(
            FirebaseAuth.getInstance().currentUser != null
        )
    }

    if (usuarioIngresado) {
        MayLePrincipal(
            onCerrarSesion = {
                FirebaseAuth.getInstance().signOut()
                usuarioIngresado = false
            }
        )
    } else {
        MayLeLogin(
            onLoginCorrecto = {
                usuarioIngresado = true
            }
        )
    }
}

@Composable
fun MayLeLogin(
    onLoginCorrecto: () -> Unit
) {

    val azulMayLe = Color(0xFF123B5D)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mostrarPassword by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var ingresando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "MayLe Ortopedia",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(50.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                mensaje = ""
            },
            label = {
                Text("Email")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                mensaje = ""
            },
            label = {
                Text("Contraseña")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (mostrarPassword) {
                androidx.compose.ui.text.input.VisualTransformation.None
            } else {
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(
                    onClick = {
                        mostrarPassword = !mostrarPassword
                    }
                ) {
                    Text(
                        text = if (mostrarPassword) {
                            "OCULTAR"
                        } else {
                            "MOSTRAR"
                        },
                        color = azulMayLe
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {

                if (email.isBlank() || password.isBlank()) {
                    mensaje = "Complete email y contraseña"
                    return@Button
                }

                ingresando = true
                mensaje = ""

                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(
                        email.trim(),
                        password
                    )
                    .addOnCompleteListener { tarea ->

                        ingresando = false

                        if (tarea.isSuccessful) {
                            onLoginCorrecto()
                        } else {
                            mensaje = "Email o contraseña incorrectos"
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (ingresando) {
                    "INGRESANDO..."
                } else {
                    "INGRESAR"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (mensaje.isNotEmpty()) {
            Text(
                text = mensaje,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MayLePrincipal(
    onCerrarSesion: () -> Unit
) {

    val azulMayLe = Color(0xFF123B5D)

    var rol by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var pantalla by remember { mutableStateOf("menu") }

    val usuario = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(usuario?.uid) {

        if (usuario != null) {

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(usuario.uid)
                .get()
                .addOnSuccessListener { documento ->
                    rol = documento.getString("role") ?: ""
                }
                .addOnFailureListener {
                    mensaje = "No se pudo obtener el tipo de usuario"
                }
        }
    }

    when (pantalla) {

        "productos" -> {
            MayLeProductosStock(
                esAdmin = rol == "admin",
                onVolver = {
                    pantalla = "menu"
                }
            )
        }

        else -> {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "MayLe Ortopedia",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = azulMayLe
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (rol == "admin") {
                        "Administrador"
                    } else {
                        "Vendedor"
                    },
                    fontSize = 18.sp,
                    color = azulMayLe
                )

                Spacer(modifier = Modifier.height(40.dp))

                MayLeModuloButton("VENTAS") {
                    mensaje = "Módulo Ventas: próximo desarrollo"
                }

                Spacer(modifier = Modifier.height(14.dp))

                MayLeModuloButton("PRODUCTOS Y STOCK") {
                    pantalla = "productos"
                    mensaje = ""
                }

                Spacer(modifier = Modifier.height(14.dp))

                MayLeModuloButton("CAJA") {
                    mensaje = "Módulo Caja: próximo desarrollo"
                }

                Spacer(modifier = Modifier.height(14.dp))

                MayLeModuloButton("COMISIONES") {
                    mensaje = "Módulo Comisiones: próximo desarrollo"
                }

                if (rol == "admin") {

                    Spacer(modifier = Modifier.height(14.dp))

                    MayLeModuloButton("ADMINISTRACIÓN") {
                        mensaje = "Módulo Administración: próximo desarrollo"
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    MayLeModuloButton("REPORTES") {
                        mensaje = "Módulo Reportes: próximo desarrollo"
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (mensaje.isNotEmpty()) {
                    Text(
                        text = mensaje,
                        color = azulMayLe,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                TextButton(
                    onClick = onCerrarSesion
                ) {
                    Text(
                        text = "CERRAR SESIÓN",
                        color = azulMayLe
                    )
                }
            }
        }
    }
}

@Composable
fun MayLeProductosStock(
    esAdmin: Boolean,
    onVolver: () -> Unit
) {

    val azulMayLe = Color(0xFF123B5D)

    var productos by remember {
        mutableStateOf(listOf<Producto>())
    }

    var mostrandoFormulario by remember {
        mutableStateOf(false)
    }

    var cargando by remember {
        mutableStateOf(true)
    }

    var mensaje by remember {
        mutableStateOf("")
    }

    fun cargarProductos() {

        cargando = true

        FirebaseFirestore.getInstance()
            .collection("products")
            .get()
            .addOnSuccessListener { resultado ->

                productos = resultado.documents.map { documento ->

                    Producto(
                        id = documento.id,
                        codigo = documento.getString("codigo") ?: "",
                        nombre = documento.getString("nombre") ?: "",
                        precioContado = documento.getDouble("precioContado") ?: 0.0,
                        precioUnPago = documento.getDouble("precioUnPago") ?: 0.0,
                        precioTresCuotas = documento.getDouble("precioTresCuotas") ?: 0.0,
                        stockTalle1 = documento.getLong("stockTalle1") ?: 0,
                        stockTalle2 = documento.getLong("stockTalle2") ?: 0,
                        stockTalle3 = documento.getLong("stockTalle3") ?: 0
                    )
                }

                cargando = false
            }
            .addOnFailureListener {

                mensaje = "No se pudieron cargar los productos"
                cargando = false
            }
    }

    LaunchedEffect(Unit) {
        cargarProductos()
    }

    if (mostrandoFormulario) {

        MayLeNuevoProducto(
            onVolver = {
                mostrandoFormulario = false
            },
            onProductoGuardado = {
                mostrandoFormulario = false
                cargarProductos()
            }
        )

    } else {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                TextButton(
                    onClick = onVolver
                ) {
                    Text("← Volver")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Productos y Stock",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = azulMayLe
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (esAdmin) {

                Button(
                    onClick = {
                        mostrandoFormulario = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("NUEVO PRODUCTO")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            if (cargando) {

                Text("Cargando productos...")

            } else if (productos.isEmpty()) {

                Text(
                    text = "Todavía no hay productos cargados.",
                    fontSize = 16.sp
                )

            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(productos) { producto ->

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                Text(
                                    text = producto.nombre,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text("Código: ${producto.codigo}")

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Efectivo: $${formatoPrecio(producto.precioContado)}"
                                )

                                Text(
                                    text = "Un pago: $${formatoPrecio(producto.precioUnPago)}"
                                )

                                Text(
                                    text = "3 cuotas: $${formatoPrecio(producto.precioTresCuotas)}"
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Talle 1: ${producto.stockTalle1} | " +
                                            "Talle 2: ${producto.stockTalle2} | " +
                                            "Talle 3: ${producto.stockTalle3}"
                                )
                            }
                        }
                    }
                }
            }

            if (mensaje.isNotEmpty()) {

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = mensaje,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
fun MayLeNuevoProducto(
    onVolver: () -> Unit,
    onProductoGuardado: () -> Unit
) {

    val azulMayLe = Color(0xFF123B5D)

    var codigo by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var precioContado by remember { mutableStateOf("") }

    var stockTalle1 by remember { mutableStateOf("0") }
    var stockTalle2 by remember { mutableStateOf("0") }
    var stockTalle3 by remember { mutableStateOf("0") }

    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    val efectivo = precioContado
        .replace(",", ".")
        .toDoubleOrNull()

    val precioUnPago = efectivo?.times(1.12) ?: 0.0
    val precioTresCuotas = efectivo?.times(1.30) ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        TextButton(
            onClick = onVolver
        ) {
            Text("← Volver")
        }

        Text(
            text = "Nuevo producto",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = azulMayLe
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = codigo,
            onValueChange = {
                codigo = it
            },
            label = {
                Text("Código interno")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
            },
            label = {
                Text("Nombre del producto")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = precioContado,
            onValueChange = {
                precioContado = it
            },
            label = {
                Text("Precio efectivo")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = if (efectivo != null) {
                formatoPrecio(precioUnPago)
            } else {
                ""
            },
            onValueChange = {},
            label = {
                Text("Precio débito/crédito 1 pago")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = if (efectivo != null) {
                formatoPrecio(precioTresCuotas)
            } else {
                ""
            },
            onValueChange = {},
            label = {
                Text("Precio crédito 3 cuotas")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Índices aplicados: 1 pago × 1,12 | 3 cuotas × 1,30",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Stock por talle",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = stockTalle1,
            onValueChange = {
                stockTalle1 = it
            },
            label = {
                Text("Stock talle 1")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = stockTalle2,
            onValueChange = {
                stockTalle2 = it
            },
            label = {
                Text("Stock talle 2")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = stockTalle3,
            onValueChange = {
                stockTalle3 = it
            },
            label = {
                Text("Stock talle 3")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {

                if (
                    codigo.isBlank() ||
                    nombre.isBlank() ||
                    efectivo == null
                ) {
                    mensaje = "Complete código, nombre y precio efectivo"
                    return@Button
                }

                guardando = true
                mensaje = ""

                val producto = hashMapOf(
                    "codigo" to codigo.trim(),
                    "nombre" to nombre.trim(),
                    "precioContado" to efectivo,
                    "precioUnPago" to precioUnPago,
                    "precioTresCuotas" to precioTresCuotas,
                    "stockTalle1" to (stockTalle1.toLongOrNull() ?: 0L),
                    "stockTalle2" to (stockTalle2.toLongOrNull() ?: 0L),
                    "stockTalle3" to (stockTalle3.toLongOrNull() ?: 0L)
                )

                FirebaseFirestore.getInstance()
                    .collection("products")
                    .add(producto)
                    .addOnSuccessListener {

                        guardando = false
                        onProductoGuardado()
                    }
                    .addOnFailureListener {

                        guardando = false
                        mensaje = "No se pudo guardar el producto"
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !guardando
        ) {

            Text(
                text = if (guardando) {
                    "GUARDANDO..."
                } else {
                    "GUARDAR PRODUCTO"
                },
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (mensaje.isNotEmpty()) {

            Text(
                text = mensaje,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MayLeModuloButton(
    texto: String,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {

        Text(
            text = texto,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}