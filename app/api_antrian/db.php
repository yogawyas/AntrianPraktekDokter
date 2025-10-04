<?php
$host = "localhost";
$user = "root"; // default XAMPP
$pass = "";     // default XAMPP biasanya kosong
$db   = "antrian_dokter";

$conn = new mysqli($host, $user, $pass, $db);

if ($conn->connect_error) {
    die("Koneksi gagal: " . $conn->connect_error);
}
?>
